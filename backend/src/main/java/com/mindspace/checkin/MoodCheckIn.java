package com.mindspace.checkin;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "mood_check_ins")
public class MoodCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "local_date", nullable = false, updatable = false)
    private LocalDate localDate;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(name = "mood_score", nullable = false)
    private int moodScore;

    @Column(name = "stress_score", nullable = false)
    private int stressScore;

    @Column(name = "energy_score", nullable = false)
    private int energyScore;

    @Column(name = "sleep_minutes", nullable = false)
    private int sleepMinutes;

    @Column(name = "sleep_quality")
    private Integer sleepQuality;

    @Column(length = 2000)
    private String notes;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "check_in_tags", joinColumns = @JoinColumn(name = "check_in_id"))
    private Set<CheckInTag> tags = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected MoodCheckIn() {
    }

    public MoodCheckIn(UUID userId, LocalDate localDate, String timezone) {
        this.userId = userId;
        this.localDate = localDate;
        this.timezone = timezone;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void apply(int mood, int stress, int energy, int sleepMinutes, Integer sleepQuality, String notes, Set<CheckInTag> newTags) {
        this.moodScore = mood;
        this.stressScore = stress;
        this.energyScore = energy;
        this.sleepMinutes = sleepMinutes;
        this.sleepQuality = sleepQuality;
        this.notes = notes;
        this.tags.clear();
        this.tags.addAll(newTags);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getLocalDate() { return localDate; }
    public String getTimezone() { return timezone; }
    public int getMoodScore() { return moodScore; }
    public int getStressScore() { return stressScore; }
    public int getEnergyScore() { return energyScore; }
    public int getSleepMinutes() { return sleepMinutes; }
    public Integer getSleepQuality() { return sleepQuality; }
    public String getNotes() { return notes; }
    public Set<CheckInTag> getTags() { return tags; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
