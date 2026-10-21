package com.mindspace.user;

import com.mindspace.auth.AuthService;
import com.mindspace.auth.PasswordPolicy;
import com.mindspace.auth.SessionService;
import com.mindspace.common.FieldViolation;
import com.mindspace.common.NotFoundException;
import com.mindspace.common.Text;
import com.mindspace.common.ValidationException;
import com.mindspace.user.UserDtos.ChangePasswordRequest;
import com.mindspace.user.UserDtos.ConsentResponse;
import com.mindspace.user.UserDtos.UpdateProfileRequest;
import com.mindspace.user.UserDtos.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository users;
    private final UserPreferenceRepository preferences;
    private final ConsentRecordRepository consents;
    private final PasswordEncoder encoder;
    private final SessionService sessions;

    public UserService(UserRepository users, UserPreferenceRepository preferences, ConsentRecordRepository consents,
                       PasswordEncoder encoder, SessionService sessions) {
        this.users = users;
        this.preferences = preferences;
        this.consents = consents;
        this.encoder = encoder;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public UserResponse profile(UUID userId) {
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User"));
        Theme theme = preferences.findById(userId).map(UserPreference::getTheme).orElse(Theme.SYSTEM);
        return toResponse(user, theme);
    }

    /** The user's timezone as a ZoneId; UTC if the stored value is somehow unusable. */
    @Transactional(readOnly = true)
    public ZoneId zoneOf(UUID userId) {
        String tz = users.findById(userId).orElseThrow(() -> new NotFoundException("User")).getTimezone();
        try {
            return ZoneId.of(tz);
        } catch (DateTimeException e) {
            return ZoneId.of("UTC");
        }
    }

    @Transactional
    public UserResponse update(UUID userId, UpdateProfileRequest req) {
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User"));
        List<FieldViolation> problems = new ArrayList<>();
        if (req.displayName() != null) {
            if (Text.isBlank(req.displayName())) {
                problems.add(new FieldViolation("displayName", "must not be blank"));
            } else {
                user.setDisplayName(Text.trim(req.displayName()));
            }
        }
        if (req.timezone() != null) {
            if (ZoneId.getAvailableZoneIds().contains(req.timezone())) {
                user.setTimezone(req.timezone());
            } else {
                problems.add(new FieldViolation("timezone", "must be a valid IANA timezone such as Asia/Kolkata"));
            }
        }
        ValidationException.throwIfAny(problems);
        Theme theme = preferences.findById(userId).map(UserPreference::getTheme).orElse(Theme.SYSTEM);
        if (req.theme() != null) {
            UserPreference pref = preferences.findById(userId).orElseGet(() -> new UserPreference(userId, Theme.SYSTEM));
            pref.setTheme(req.theme());
            preferences.save(pref);
            theme = req.theme();
        }
        return toResponse(users.save(user), theme);
    }

    /** Verifies the password of an authenticated user (used before sensitive operations). */
    @Transactional(readOnly = true)
    public boolean passwordMatches(UUID userId, String candidate) {
        if (candidate == null || candidate.getBytes(StandardCharsets.UTF_8).length > PasswordPolicy.MAX_BYTES) {
            return false;
        }
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User"));
        return encoder.matches(candidate, user.getPasswordHash());
    }

    @Transactional
    public void changePassword(UUID userId, UUID currentFamilyId, ChangePasswordRequest req) {
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User"));
        List<FieldViolation> problems = new ArrayList<>();
        if (!passwordMatches(userId, req.currentPassword())) {
            problems.add(new FieldViolation("currentPassword", "is incorrect"));
        }
        if (!req.newPassword().equals(req.confirmPassword())) {
            problems.add(new FieldViolation("confirmPassword", "does not match the new password"));
        }
        if (req.newPassword().equals(req.currentPassword())) {
            problems.add(new FieldViolation("newPassword", "must be different from the current password"));
        }
        problems.addAll(PasswordPolicy.validate(req.newPassword(), user.getEmail(), user.getDisplayName()).stream()
                .map(v -> new FieldViolation("newPassword", v.message())).toList());
        ValidationException.throwIfAny(problems);

        user.changePasswordHash(encoder.encode(req.newPassword()));
        users.save(user);
        // Every other device must sign in again with the new password.
        sessions.revokeOthers(userId, currentFamilyId, "PASSWORD_CHANGE");
    }

    @Transactional(readOnly = true)
    public ConsentResponse consents(UUID userId) {
        return new ConsentResponse(
                isGranted(userId, ConsentType.PRIVACY_NOTICE_ACK),
                isGranted(userId, ConsentType.WELLNESS_DISCLAIMER_ACK),
                isGranted(userId, ConsentType.OPTIONAL_USAGE_ANALYTICS),
                AuthService.POLICY_VERSION);
    }

    @Transactional
    public ConsentResponse setUsageAnalyticsConsent(UUID userId, boolean granted) {
        consents.save(new ConsentRecord(userId, ConsentType.OPTIONAL_USAGE_ANALYTICS, granted, AuthService.POLICY_VERSION));
        return consents(userId);
    }

    private boolean isGranted(UUID userId, ConsentType type) {
        return consents.findFirstByUserIdAndConsentTypeOrderByRecordedAtDesc(userId, type)
                .map(ConsentRecord::isGranted).orElse(false);
    }

    private static UserResponse toResponse(User u, Theme theme) {
        return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getTimezone(), theme, u.getCreatedAt());
    }
}
