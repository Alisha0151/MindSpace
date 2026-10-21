package com.mindspace.auth;

import com.mindspace.common.NotFoundException;
import com.mindspace.config.AppProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Refresh-token lifecycle: issue, rotate, revoke.
 *
 * <p>Each refresh token is single-use. Presenting it again after it was rotated is treated as theft:
 * the whole family is revoked (REUSE_DETECTED). A short grace window tolerates two browser tabs refreshing
 * at the same moment without logging the user out.
 */
@Service
public class SessionService {

    static final Duration ROTATION_GRACE = Duration.ofSeconds(10);

    private final RefreshSessionRepository sessions;
    private final AppProperties.Security config;
    private final Clock clock;

    public SessionService(RefreshSessionRepository sessions, AppProperties props, Clock clock) {
        this.sessions = sessions;
        this.config = props.security();
        this.clock = clock;
    }

    /** A freshly issued refresh token (raw value, shown once) and the family it belongs to. */
    public record Issued(UUID userId, UUID familyId, String refreshToken) {
    }

    @Transactional
    public Issued start(UUID userId, String userAgent) {
        return issue(userId, UUID.randomUUID(), userAgent).issued();
    }

    /** noRollbackFor: the family revocation on reuse detection must be committed even though we then reject the request. */
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public Issued rotate(String rawToken, String userAgent) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthenticationFailedException();
        }
        RefreshSession current = sessions.findByTokenHash(TokenHasher.sha256Hex(rawToken))
                .orElseThrow(AuthenticationFailedException::new);
        Instant now = clock.instant();

        if (current.isRevoked()) {
            boolean justRotated = "ROTATED".equals(current.getRevokedReason())
                    && Duration.between(current.getRevokedAt(), now).compareTo(ROTATION_GRACE) <= 0;
            if (!justRotated) {
                sessions.revokeFamily(current.getFamilyId(), now, "REUSE_DETECTED");
            }
            throw new AuthenticationFailedException();
        }
        if (current.isExpired(now)) {
            throw new AuthenticationFailedException();
        }

        current.revoke(now, "ROTATED");
        IssuedRow next = issue(current.getUserId(), current.getFamilyId(), userAgent);
        current.setReplacedBy(next.row().getId());
        return next.issued();
    }

    @Transactional
    public void logoutByRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        sessions.findByTokenHash(TokenHasher.sha256Hex(rawToken))
                .ifPresent(s -> sessions.revokeFamily(s.getFamilyId(), clock.instant(), "LOGOUT"));
    }

    @Transactional
    public void logoutFamily(UUID familyId) {
        sessions.revokeFamily(familyId, clock.instant(), "LOGOUT");
    }

    @Transactional
    public void logoutAll(UUID userId) {
        sessions.revokeAllForUser(userId, clock.instant(), "LOGOUT", null);
    }

    @Transactional
    public void revokeOthers(UUID userId, UUID keepFamilyId, String reason) {
        sessions.revokeAllForUser(userId, clock.instant(), reason, keepFamilyId);
    }

    @Transactional(readOnly = true)
    public List<RefreshSession> activeSessions(UUID userId) {
        return sessions.findByUserIdAndRevokedAtIsNullOrderByIssuedAtDesc(userId);
    }

    /** Ownership-checked: another user's session id looks exactly like a missing one. */
    @Transactional
    public void revokeOwned(UUID userId, UUID sessionId) {
        RefreshSession session = sessions.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Session"));
        sessions.revokeFamily(session.getFamilyId(), clock.instant(), "LOGOUT");
    }

    @Transactional(readOnly = true)
    public boolean isFamilyActive(UUID familyId) {
        return sessions.existsByFamilyIdAndRevokedAtIsNull(familyId);
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpired() {
        sessions.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(1)));
    }

    private IssuedRow issue(UUID userId, UUID familyId, String userAgent) {
        String token = TokenHasher.newToken();
        Instant now = clock.instant();
        RefreshSession row = sessions.save(new RefreshSession(userId, familyId, TokenHasher.sha256Hex(token),
                now, now.plus(config.refreshTokenTtl()), userAgent));
        return new IssuedRow(new Issued(userId, familyId, token), row);
    }

    private record IssuedRow(Issued issued, RefreshSession row) {
    }
}
