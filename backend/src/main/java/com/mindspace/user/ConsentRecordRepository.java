package com.mindspace.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {

    Optional<ConsentRecord> findFirstByUserIdAndConsentTypeOrderByRecordedAtDesc(UUID userId, ConsentType type);

    List<ConsentRecord> findByUserIdOrderByRecordedAtAsc(UUID userId);
}
