package com.mindspace.checkin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Every method takes the owning user id: there is intentionally no finder by id alone. */
public interface CheckInRepository extends JpaRepository<MoodCheckIn, UUID> {

    Optional<MoodCheckIn> findByIdAndUserId(UUID id, UUID userId);

    Optional<MoodCheckIn> findByUserIdAndLocalDate(UUID userId, LocalDate localDate);

    boolean existsByUserIdAndLocalDate(UUID userId, LocalDate localDate);

    List<MoodCheckIn> findByUserIdAndLocalDateBetweenOrderByLocalDateAsc(UUID userId, LocalDate from, LocalDate to);

    List<MoodCheckIn> findByUserIdOrderByLocalDateAsc(UUID userId);

    /** The database cascades to check_in_tags. */
    @Modifying
    @Query("delete from MoodCheckIn c where c.id = :id and c.userId = :userId")
    int deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
