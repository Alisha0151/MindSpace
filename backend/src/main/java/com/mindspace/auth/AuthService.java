package com.mindspace.auth;

import com.mindspace.auth.AuthDtos.LoginRequest;
import com.mindspace.auth.AuthDtos.RegisterRequest;
import com.mindspace.common.ConflictException;
import com.mindspace.common.FieldViolation;
import com.mindspace.common.RateLimitedException;
import com.mindspace.common.Text;
import com.mindspace.common.ValidationException;
import com.mindspace.common.ratelimit.RateLimiter;
import com.mindspace.config.AppProperties;
import com.mindspace.user.ConsentRecord;
import com.mindspace.user.ConsentRecordRepository;
import com.mindspace.user.ConsentType;
import com.mindspace.user.Theme;
import com.mindspace.user.User;
import com.mindspace.user.UserPreference;
import com.mindspace.user.UserPreferenceRepository;
import com.mindspace.user.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Locale;

@Service
public class AuthService {

    public static final String POLICY_VERSION = "2026-10";

    private final UserRepository users;
    private final UserPreferenceRepository preferences;
    private final ConsentRecordRepository consents;
    private final PasswordEncoder encoder;
    private final SessionService sessions;
    private final JwtService jwt;
    private final RateLimiter failedLoginLimiter;
    /** Verified against when the e-mail is unknown, so response time does not reveal whether an account exists. */
    private final String dummyHash;

    public AuthService(UserRepository users, UserPreferenceRepository preferences, ConsentRecordRepository consents,
                       PasswordEncoder encoder, SessionService sessions, JwtService jwt, AppProperties props) {
        this.users = users;
        this.preferences = preferences;
        this.consents = consents;
        this.encoder = encoder;
        this.sessions = sessions;
        this.jwt = jwt;
        AppProperties.RateLimit rl = props.rateLimit();
        this.failedLoginLimiter = new RateLimiter(rl.loginEmailMax(), rl.loginEmailWindow());
        this.dummyHash = encoder.encode("not-a-real-password-used-for-timing-only");
    }

    /** Result of a successful register/login: the user plus the raw credentials to put in cookies. */
    public record AuthResult(UUID userId, String accessToken, String refreshToken) {
    }

    @Transactional
    public AuthResult register(RegisterRequest req, String userAgent) {
        List<FieldViolation> problems = new ArrayList<>();
        String email = null;
        try {
            email = Text.normalizeEmail(req.email());
        } catch (IllegalArgumentException e) {
            problems.add(new FieldViolation("email", "must be a valid e-mail address"));
        }
        String displayName = Text.trim(req.displayName());
        if (Text.isBlank(displayName)) {
            problems.add(new FieldViolation("displayName", "must not be blank"));
        }
        if (!isValidZone(req.timezone())) {
            problems.add(new FieldViolation("timezone", "must be a valid IANA timezone such as Asia/Kolkata"));
        }
        if (!req.password().equals(req.confirmPassword())) {
            problems.add(new FieldViolation("confirmPassword", "does not match the password"));
        }
        problems.addAll(PasswordPolicy.validate(req.password(), email, displayName));
        if (!req.acceptTerms()) {
            problems.add(new FieldViolation("acceptTerms", "you must acknowledge the privacy notice and wellness disclaimer"));
        }
        ValidationException.throwIfAny(problems);

        if (users.existsByEmail(email)) {
            throw new ConflictException("An account with this e-mail already exists");
        }
        User user = users.save(new User(email, displayName, encoder.encode(req.password()), req.timezone()));
        preferences.save(new UserPreference(user.getId(), Theme.SYSTEM));
        consents.save(new ConsentRecord(user.getId(), ConsentType.PRIVACY_NOTICE_ACK, true, POLICY_VERSION));
        consents.save(new ConsentRecord(user.getId(), ConsentType.WELLNESS_DISCLAIMER_ACK, true, POLICY_VERSION));
        return issue(user, userAgent);
    }

    /** No class-level @Transactional: failed attempts must be recorded even though the call ends in an exception. */
    public AuthResult login(LoginRequest req, String userAgent) {
        String key = req.email().strip().toLowerCase(Locale.ROOT);
        long wait = failedLoginLimiter.retryAfterSeconds(key);
        if (wait > 0) {
            throw new RateLimitedException(wait);
        }

        User user = users.findByEmail(key).orElse(null);
        boolean tooLong = req.password().getBytes(StandardCharsets.UTF_8).length > PasswordPolicy.MAX_BYTES;
        boolean matches = !tooLong && encoder.matches(req.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !matches) {
            failedLoginLimiter.tryAcquire(key);
            throw new AuthenticationFailedException();
        }
        return issueInTransaction(user, userAgent);
    }

    public AuthResult refresh(String rawRefreshToken, String userAgent) {
        SessionService.Issued issued = sessions.rotate(rawRefreshToken, userAgent);
        return new AuthResult(issued.userId(), jwt.issue(issued.userId(), issued.familyId()), issued.refreshToken());
    }

    @Scheduled(fixedDelay = 600_000)
    void purgeLimiter() {
        failedLoginLimiter.purgeIdle();
    }

    private AuthResult issueInTransaction(User user, String userAgent) {
        return issue(user, userAgent);
    }

    private AuthResult issue(User user, String userAgent) {
        SessionService.Issued issued = sessions.start(user.getId(), userAgent);
        return new AuthResult(user.getId(), jwt.issue(user.getId(), issued.familyId()), issued.refreshToken());
    }

    static boolean isValidZone(String timezone) {
        try {
            return timezone != null && ZoneId.getAvailableZoneIds().contains(timezone) && ZoneId.of(timezone) != null;
        } catch (DateTimeException e) {
            return false;
        }
    }
}
