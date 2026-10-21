package com.mindspace.checkin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CheckInDtos {

    private CheckInDtos() {
    }

    @Schema(description = "Scores are self-reported observations, not clinical measures. mood 1=very low..5=very good; stress 1=very calm..5=very stressed; energy 1=very low..5=very high; sleepQuality 1=very poor..5=very good.")
    public record CheckInRequest(
            @Schema(description = "Local calendar date; defaults to today in the user's timezone. Cannot be in the future. Ignored on update.", example = "2026-10-17")
            LocalDate date,
            @Schema(example = "4") Integer moodScore,
            @Schema(example = "2") Integer stressScore,
            @Schema(example = "3") Integer energyScore,
            @Schema(description = "Minutes slept, 0..1440", example = "450") Integer sleepMinutes,
            Integer sleepQuality,
            @Size(max = 10) List<@Size(max = 50) String> emotions,
            @Size(max = 10) List<@Size(max = 50) String> activities,
            @Size(max = 10) List<@Size(max = 50) String> contexts,
            @Size(max = 4000) String notes) {
    }

    public record CheckInResponse(UUID id, LocalDate date, String timezone, int moodScore, int stressScore,
                                  int energyScore, int sleepMinutes, Integer sleepQuality,
                                  List<String> emotions, List<String> activities, List<String> contexts,
                                  String notes, Instant createdAt, Instant updatedAt) {
    }
}
