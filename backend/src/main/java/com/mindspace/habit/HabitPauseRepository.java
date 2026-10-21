package com.mindspace.habit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HabitPauseRepository extends JpaRepository<HabitPause, UUID> {

    List<HabitPause> findByUserId(UUID userId);

    List<HabitPause> findByHabitIdAndUserId(UUID habitId, UUID userId);

    Optional<HabitPause> findFirstByHabitIdAndUserIdAndResumedOnIsNull(UUID habitId, UUID userId);
}
