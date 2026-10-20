package com.affiliatelink.profile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdvertiserRequest(

        @NotBlank @Email String email,
        @NotBlank @Size(max=55) String companyName,
        @Size(max = 55) String sector,
        @Size(max = 55) String website
) {
}
