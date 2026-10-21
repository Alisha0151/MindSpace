package com.mindspace.habit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A pause window [pausedOn, resumedOn). The database allows at most one open pause per habit. */
@Entity
@Table(name = "habit_pauses")
public class HabitPause {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "habit_id", nullable = false, updatable = false)
    private UUID habitId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "paused_on", nullable = false, updatable = false)
    private LocalDate pausedOn;

    @Column(name = "resumed_on")
    private LocalDate resumedOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected HabitPause() {
    }

    public HabitPause(UUID habitId, UUID userId, LocalDate pausedOn) {
        this.habitId = habitId;
        this.userId = userId;
        this.pausedOn = pausedOn;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getHabitId() { return habitId; }
    public LocalDate getPausedOn() { return pausedOn; }
    public LocalDate getResumedOn() { return resumedOn; }
    public void setResumedOn(LocalDate resumedOn) { this.resumedOn = resumedOn; }
}
