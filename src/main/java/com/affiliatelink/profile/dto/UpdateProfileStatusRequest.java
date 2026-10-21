package com.affiliatelink.profile.dto;

import com.affiliatelink.profile.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateProfileStatusRequest(
        @NotNull AccountStatus status
) {
}
