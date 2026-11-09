package com.affiliatelink.offer.dto;

import com.affiliatelink.offer.entity.PayoutModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CampaignRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @Positive BigDecimal budget,
        @NotNull PayoutModel payoutModel,
        @NotNull BigDecimal payoutValue
        ) {
}
