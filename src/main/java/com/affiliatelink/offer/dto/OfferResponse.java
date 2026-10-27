package com.affiliatelink.offer.dto;

import com.affiliatelink.profile.Niche;
import java.time.LocalDateTime;
import java.util.Set;

public record OfferResponse(

        Long id,
        Long advertiserId,
        String title,
        String description,
        String landingUrl,
        Niche niche,
        Set<String> tags,
        LocalDateTime createdAt
) {
}
