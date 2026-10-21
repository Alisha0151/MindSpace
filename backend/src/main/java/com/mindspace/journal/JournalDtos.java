package com.mindspace.journal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class JournalDtos {

    private JournalDtos() {
    }

    public record JournalRequest(@Size(max = 200) String title,
                                 @NotBlank @Size(max = 10000) String body,
                                 @Size(max = 300) String promptText,
                                 @Size(max = 10) List<@Size(max = 50) String> tags) {
    }

    /** List item: a short excerpt, not the full text. */
    public record JournalSummary(UUID id, LocalDate entryDate, String title, String excerpt, List<String> tags,
                                 Instant createdAt, Instant updatedAt) {
    }

    public record JournalDetail(UUID id, LocalDate entryDate, String title, String body, String promptText,
                                List<String> tags, Instant createdAt, Instant updatedAt) {
    }
}
