package com.affiliatelink.profile.dto;

import com.affiliatelink.profile.entity.AccountStatus;

import java.time.LocalDateTime;

public record AdvertierResponse(

        Long id,
        String email,
        String companyName,
        String sector,
        String website,
        AccountStatus status,
        LocalDateTime createdAt

) {
}
