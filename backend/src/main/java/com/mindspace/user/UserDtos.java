package com.mindspace.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(UUID id, String email, String displayName, String timezone, Theme theme, Instant createdAt) {
    }

    /** Only these fields can be changed through the profile endpoint (no mass assignment of e-mail, hash, id...). */
    public record UpdateProfileRequest(@Size(max = 100) String displayName, @Size(max = 64) String timezone, Theme theme) {
    }

    public record ChangePasswordRequest(@NotBlank @Size(max = 200) String currentPassword,
                                        @NotBlank @Size(max = 200) String newPassword,
                                        @NotBlank @Size(max = 200) String confirmPassword) {
    }

    public record ConsentResponse(boolean privacyNoticeAcknowledged, boolean wellnessDisclaimerAcknowledged,
                                  boolean optionalUsageAnalytics, String policyVersion) {
    }

    public record UpdateConsentRequest(@NotNull Boolean optionalUsageAnalytics) {
    }
}
