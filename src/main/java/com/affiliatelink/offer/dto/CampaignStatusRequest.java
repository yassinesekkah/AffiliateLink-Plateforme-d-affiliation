package com.affiliatelink.offer.dto;

import com.affiliatelink.offer.entity.CampaignStatus;
import jakarta.validation.constraints.NotNull;

public record CampaignStatusRequest(
        @NotNull CampaignStatus status
) {
}
