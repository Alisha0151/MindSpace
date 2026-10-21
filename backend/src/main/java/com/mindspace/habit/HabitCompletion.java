package com.mindspace.habit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Read model for habit_completions. Rows are created through {@link HabitCompletionRepository#insertIfAbsent}. */
@Entity
@Table(name = "habit_completions")
public class HabitCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "habit_id", nullable = false, updatable = false)
    private UUID habitId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "completed_on", nullable = false, updatable = false)
    private LocalDate completedOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected HabitCompletion() {
    }

    public UUID getId() { return id; }
    public UUID getHabitId() { return habitId; }
    public LocalDate getCompletedOn() { return completedOn; }
    public Instant getCreatedAt() { return createdAt; }
}
