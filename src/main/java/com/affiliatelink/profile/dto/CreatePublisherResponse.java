package com.affiliatelink.profile.dto;

import com.affiliatelink.profile.entity.AccountStatus;
import com.affiliatelink.profile.Niche;

import java.time.LocalDateTime;

public record CreatePublisherResponse(

        Long id,
        String email,
        String firstName,
        String lastName,
        Niche niche,
        String bio,
        String country,
        AccountStatus status,
        LocalDateTime createdAt
) {
}
