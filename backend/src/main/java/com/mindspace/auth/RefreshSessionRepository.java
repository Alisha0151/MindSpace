package com.mindspace.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    /** Per-request session check; served by refresh_sessions_family_active_idx (V6). */
    boolean existsByFamilyIdAndRevokedAtIsNull(UUID familyId);

    List<RefreshSession> findByUserIdAndRevokedAtIsNullOrderByIssuedAtDesc(UUID userId);

    Optional<RefreshSession> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("update RefreshSession s set s.revokedAt = :now, s.revokedReason = :reason "
            + "where s.familyId = :familyId and s.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now, @Param("reason") String reason);

    @Modifying
    @Query("update RefreshSession s set s.revokedAt = :now, s.revokedReason = :reason "
            + "where s.userId = :userId and s.revokedAt is null and (:exceptFamily is null or s.familyId <> :exceptFamily)")
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now,
                         @Param("reason") String reason, @Param("exceptFamily") UUID exceptFamily);

    @Modifying
    @Query("delete from RefreshSession s where s.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
