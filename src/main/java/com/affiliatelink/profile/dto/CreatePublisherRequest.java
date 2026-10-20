package com.affiliatelink.profile.dto;

import com.affiliatelink.profile.entity.Niche;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePublisherRequest(
         @Email @NotBlank String email,
         @NotBlank @Size(max = 55) String firstName,
         @NotBlank @Size(max = 55) String lastName,
         @NotNull Niche niche,
         @Size(max = 255) String bio,
         @Size(max = 55) String country

) {
}
