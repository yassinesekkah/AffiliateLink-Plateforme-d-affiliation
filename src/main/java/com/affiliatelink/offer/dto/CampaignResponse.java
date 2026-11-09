package com.affiliatelink.offer.dto;

import com.affiliatelink.offer.entity.CampaignStatus;
import com.affiliatelink.offer.entity.PayoutModel;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CampaignResponse(
        Long id,
        Long offerId,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budget,
        PayoutModel payoutModel,
        BigDecimal payoutValue,
        CampaignStatus status
) {
}
