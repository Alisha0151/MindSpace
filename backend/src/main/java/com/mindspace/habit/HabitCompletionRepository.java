package com.mindspace.habit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface HabitCompletionRepository extends JpaRepository<HabitCompletion, UUID> {

    interface HabitDate {
        UUID getHabitId();

        LocalDate getCompletedOn();
    }

    /** All completion dates for several of one user's habits in a single query (avoids N+1). */
    @Query("select c.habitId as habitId, c.completedOn as completedOn from HabitCompletion c "
            + "where c.userId = :userId and c.habitId in :habitIds")
    List<HabitDate> findDates(@Param("userId") UUID userId, @Param("habitIds") Collection<UUID> habitIds);

    @Query("select c.completedOn from HabitCompletion c where c.habitId = :habitId and c.userId = :userId "
            + "and c.completedOn between :from and :to order by c.completedOn")
    List<LocalDate> findDatesInRange(@Param("userId") UUID userId, @Param("habitId") UUID habitId,
                                     @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<HabitCompletion> findByUserIdOrderByCompletedOnAsc(UUID userId);

    /** Idempotent and race-safe: the unique key (habit_id, completed_on) turns a repeat into a no-op. */
    @Modifying
    @Query(value = "INSERT INTO habit_completions (id, habit_id, user_id, completed_on, created_at) "
            + "VALUES (gen_random_uuid(), :habitId, :userId, :day, now()) "
            + "ON CONFLICT (habit_id, completed_on) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("habitId") UUID habitId, @Param("day") LocalDate day);

    @Modifying
    @Query("delete from HabitCompletion c where c.habitId = :habitId and c.userId = :userId and c.completedOn = :day")
    int deleteCompletion(@Param("userId") UUID userId, @Param("habitId") UUID habitId, @Param("day") LocalDate day);
}
