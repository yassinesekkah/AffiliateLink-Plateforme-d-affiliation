param([switch]$Verify)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$server = 'http://localhost:9000'
$localDirectory = Join-Path $projectRoot '.sonar-local'
New-Item -ItemType Directory -Force -Path $localDirectory | Out-Null
$credentialsPath = Join-Path $localDirectory 'credentials.json'

function Get-AuthHeaders($login, $password) {
    $encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${login}:${password}"))
    return @{ Authorization = "Basic $encoded" }
}

if (Test-Path -LiteralPath $credentialsPath) {
    $credentials = Get-Content -LiteralPath $credentialsPath -Raw | ConvertFrom-Json
} else {
    # Only bootstrap the new local instance configured by compose.sonar.yaml.
    $password = 'Local-Sonar-' + [Guid]::NewGuid().ToString('N') + '!'
    $initialHeaders = Get-AuthHeaders 'admin' 'admin'
    Invoke-RestMethod -Method Post -Uri "$server/api/users/change_password" -Headers $initialHeaders `
        -Body @{ login = 'admin'; previousPassword = 'admin'; password = $password } | Out-Null
    $credentials = @{ login = 'admin'; password = $password }
    $credentials | ConvertTo-Json | Set-Content -LiteralPath $credentialsPath
}

$headers = Get-AuthHeaders $credentials.login $credentials.password
$tokenName = 'local-analysis-' + [Guid]::NewGuid().ToString('N')
$tokenResult = Invoke-RestMethod -Method Post -Uri "$server/api/user_tokens/generate" `
    -Headers $headers -Body @{ name = $tokenName }
$previousToken = $env:SONAR_TOKEN
try {
    $env:SONAR_TOKEN = $tokenResult.token
    # Windows PowerShell otherwise treats some native stderr warnings as terminating errors.
    $ErrorActionPreference = 'Continue'
    $maven = if (Get-Command mvn -ErrorAction SilentlyContinue) { 'mvn' } else { '.\mvnw.cmd' }
    if ($Verify) { & $maven -B clean verify sonar:sonar } else { & $maven -B sonar:sonar }
    $scannerExit = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($scannerExit -ne 0) { throw "SonarScanner failed with exit code $scannerExit" }

    $taskProperties = ConvertFrom-StringData (Get-Content -LiteralPath 'target/sonar/report-task.txt' -Raw)
    $deadline = (Get-Date).AddMinutes(3)
    do {
        $task = (Invoke-RestMethod -Uri $taskProperties.ceTaskUrl -Headers $headers).task
        if ($task.status -in @('SUCCESS', 'FAILED', 'CANCELED')) { break }
        if ((Get-Date) -ge $deadline) { throw 'Timed out waiting for SonarQube processing' }
        Start-Sleep -Seconds 3
    } while ($true)
    if ($task.status -ne 'SUCCESS') { throw "SonarQube processing failed: $($task.status)" }

    $metrics = Invoke-RestMethod -Headers $headers -Uri (
        "$server/api/measures/component?component=affiliatelink&metricKeys=bugs,vulnerabilities,code_smells,coverage,security_hotspots")
    $gate = Invoke-RestMethod -Headers $headers -Uri "$server/api/qualitygates/project_status?analysisId=$($task.analysisId)"
    $summary = @{ analysisId = $task.analysisId; dashboard = "$server/dashboard?id=affiliatelink";
        qualityGate = $gate.projectStatus.status; measures = $metrics.component.measures }
    $summary | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $localDirectory 'analysis-summary.json')
    $summary | ConvertTo-Json -Depth 8
} finally {
    $env:SONAR_TOKEN = $previousToken
    Invoke-RestMethod -Method Post -Uri "$server/api/user_tokens/revoke" -Headers $headers `
        -Body @{ name = $tokenName } | Out-Null
}
