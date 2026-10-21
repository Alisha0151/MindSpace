package com.mindspace.habit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HabitRepository extends JpaRepository<Habit, UUID> {

    List<Habit> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<Habit> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    /** The database cascades to pauses and completions. */
    @Modifying
    @Query("delete from Habit h where h.id = :id and h.userId = :userId")
    int deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
