package com.mindspace.journal;

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

/**
 * Journal text is sensitive and plain text only. The generated search_vector column is intentionally not mapped;
 * it is only used by the native search query in {@link JournalRepository}.
 */
@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "entry_date", nullable = false, updatable = false)
    private LocalDate entryDate;

    @Column(length = 200)
    private String title;

    @Column(nullable = false, length = 10000)
    private String body;

    @Column(name = "prompt_text", length = 300)
    private String promptText;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "journal_entry_tags", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected JournalEntry() {
    }

    public JournalEntry(UUID userId, LocalDate entryDate) {
        this.userId = userId;
        this.entryDate = entryDate;
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

    public void apply(String title, String body, String promptText, Set<String> newTags) {
        this.title = title;
        this.body = body;
        this.promptText = promptText;
        this.tags.clear();
        this.tags.addAll(newTags);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getEntryDate() { return entryDate; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getPromptText() { return promptText; }
    public Set<String> getTags() { return tags; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
