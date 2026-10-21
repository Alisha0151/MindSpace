package com.mindspace.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One issued refresh token (only its SHA-256 digest is stored). Rotation creates a new row in the same family. */
@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 30)
    private String revokedReason;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    protected RefreshSession() {
    }

    public RefreshSession(UUID userId, UUID familyId, String tokenHash, Instant issuedAt, Instant expiresAt, String userAgent) {
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent == null ? null : userAgent.substring(0, Math.min(255, userAgent.length()));
    }

    public void revoke(Instant when, String reason) {
        if (revokedAt == null) {
            this.revokedAt = when;
            this.revokedReason = reason;
        }
    }

    public boolean isRevoked() { return revokedAt != null; }
    public boolean isExpired(Instant now) { return !expiresAt.isAfter(now); }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getFamilyId() { return familyId; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getRevokedReason() { return revokedReason; }
    public String getUserAgent() { return userAgent; }
    public void setReplacedBy(UUID replacedBy) { this.replacedBy = replacedBy; }
}
