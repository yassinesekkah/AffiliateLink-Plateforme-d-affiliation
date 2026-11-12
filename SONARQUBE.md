# SonarQube local

Demarrer Docker Desktop, puis depuis la racine du projet :

```powershell
docker compose -f compose.sonar.yaml -p affiliatelink-sonar up -d
```

Attendre que http://localhost:9000/api/system/status indique `UP`. Le serveur et sa base
PostgreSQL utilisent des volumes dedies. Le fichier Compose est separe de celui
de l'application pour ne pas lancer SonarQube pendant les tests.

Sur une nouvelle installation, le compte initial est `admin` / `admin` et le mot
de passe doit etre change. Si l'installation a ete initialisee par l'agent, les
identifiants locaux sont dans `.sonar-local/credentials.json` (ignore par Git).

Pour initialiser le compte local et analyser automatiquement :

```powershell
.\scripts\Invoke-LocalSonarAnalysis.ps1 -Verify
```

Le script initialise le mot de passe administrateur de cette nouvelle instance,
conserve les identifiants dans `.sonar-local/credentials.json`, genere un jeton
temporaire et le revoque apres l'analyse. Un resume est ecrit dans
`.sonar-local/analysis-summary.json`. Sans `-Verify`, il reutilise les classes et
le rapport JaCoCo du dernier build reussi sur le meme code.

Pour executer Maven manuellement, dans My Account > Security, generer un jeton
d'analyse. Le fournir via la variable
d'environnement `SONAR_TOKEN` dans le terminal, sans le commiter.

```powershell
mvn clean verify sonar:sonar
```

Si `mvn clean verify` vient deja de reussir sur le meme code, `mvn sonar:sonar`
suffit. Le scanner importe `target/site/jacoco/jacoco.xml` et analyse les sources
et tests identifies par Maven. Les exclusions de couverture correspondent a
celles de JaCoCo ; elles ne masquent pas les problemes de qualite de ces fichiers.

Dashboard : http://localhost:9000/dashboard?id=affiliatelink

Pour arreter les services en conservant les analyses :

```powershell
docker compose -f compose.sonar.yaml -p affiliatelink-sonar down
```

Configuration reservee au developpement local : port lie a localhost et controles
Elasticsearch de production desactives pour Docker Desktop.

## Analyse du 12 novembre 2026

- SonarQube Community Build : `26.11.0.134175` (image epinglee par digest).
- Profil Java : `Sonar way core`.
- 55 fichiers Java principaux et 18 fichiers de tests analyses.
- Rapport JaCoCo importe ; couverture affichee par SonarQube : 88,4 %.
- Bugs : 0 ; vulnerabilites : 0 ; security hotspots : 0.
- Quality Gate : `OK`.
- Analyse : `f371bcf0-bcae-4105-befb-963c13bfc1eb`.

Ces resultats correspondent au code analyse et aux regles actives de ce profil.
Le controle Maven JaCoCo reste base sur les lignes (83,63 % lors du dernier
`mvn clean verify`) ; SonarQube utilise son propre calcul de couverture.
