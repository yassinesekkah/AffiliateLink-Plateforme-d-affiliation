package com.affiliatelink.offer.dto;

import com.affiliatelink.profile.Niche;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.Set;

public record OfferRequest(
        @NotNull @Positive Long advertiserId,
        @NotBlank @Size(max = 255) String title,
        @NotBlank  String description,
        @NotBlank @Size(max = 255) @URL String landingUrl,
        @NotNull Niche niche,
        Set<@NotBlank String> tags
) {
}
