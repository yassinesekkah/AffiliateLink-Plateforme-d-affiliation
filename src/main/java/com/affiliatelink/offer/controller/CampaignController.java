package com.affiliatelink.offer.controller;

import com.affiliatelink.offer.dto.CampaignResponse;
import com.affiliatelink.offer.entity.CampaignStatus;
import com.affiliatelink.offer.service.CampaignService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
