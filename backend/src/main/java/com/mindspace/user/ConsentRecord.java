package com.mindspace.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Append-only log; the newest row per (user, type) is the current decision. */
@Entity
@Table(name = "consent_records")
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 40)
    private ConsentType consentType;

    @Column(nullable = false)
    private boolean granted;

    @Column(name = "policy_version", nullable = false, length = 20)
    private String policyVersion;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected ConsentRecord() {
    }

    public ConsentRecord(UUID userId, ConsentType consentType, boolean granted, String policyVersion) {
        this.userId = userId;
        this.consentType = consentType;
        this.granted = granted;
        this.policyVersion = policyVersion;
        this.recordedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public ConsentType getConsentType() { return consentType; }
    public boolean isGranted() { return granted; }
    public String getPolicyVersion() { return policyVersion; }
    public Instant getRecordedAt() { return recordedAt; }
}
