package com.mindspace.user;

import com.mindspace.auth.AuthUser;
import com.mindspace.user.UserDtos.ChangePasswordRequest;
import com.mindspace.user.UserDtos.ConsentResponse;
import com.mindspace.user.UserDtos.UpdateConsentRequest;
import com.mindspace.user.UserDtos.UpdateProfileRequest;
import com.mindspace.user.UserDtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "Profile and settings")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @Operation(summary = "Current user profile and theme preference")
    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return users.profile(user.userId());
    }

    @Operation(summary = "Update display name, timezone and/or theme", description = "Only supplied fields change. Timezone must be an IANA id; it decides what 'today' means for new check-ins.")
    @PatchMapping
    public UserResponse update(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody UpdateProfileRequest request) {
        return users.update(user.userId(), request);
    }

    @Operation(summary = "Change password", description = "Requires the current password. Signs out every other device.")
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthUser user,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        users.changePassword(user.userId(), user.sessionFamilyId(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consent decisions")
    @GetMapping("/consents")
    public ConsentResponse consents(@AuthenticationPrincipal AuthUser user) {
        return users.consents(user.userId());
    }

    @Operation(summary = "Record the optional usage-analytics preference", description = "No usage analytics are currently collected; this stores your preference for any future feature.")
    @PutMapping("/consents")
    public ConsentResponse setConsent(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody UpdateConsentRequest request) {
        return users.setUsageAnalyticsConsent(user.userId(), request.optionalUsageAnalytics());
    }
}
