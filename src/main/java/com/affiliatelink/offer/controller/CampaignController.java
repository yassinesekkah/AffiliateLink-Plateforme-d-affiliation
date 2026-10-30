package com.affiliatelink.offer.controller;

import com.affiliatelink.offer.dto.CampaignResponse;
import com.affiliatelink.offer.dto.CampaignStatusRequest;
import com.affiliatelink.offer.entity.CampaignStatus;
import com.affiliatelink.offer.service.CampaignService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    public List<CampaignResponse> getCampaigns(
            @RequestParam(required = false) CampaignStatus status
    ){
        return campaignService.getAll(status);
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody CampaignStatusRequest request
    ) {
        campaignService.updateStatus(id, request.status());
    }
}
