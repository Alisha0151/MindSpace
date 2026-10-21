package com.mindspace.wellness;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class WellnessDtos {

    private WellnessDtos() {
    }

    public record CopingRequest(@NotBlank @Size(max = 120) String title, @Size(max = 2000) String details) {
    }

    public record CopingResponse(UUID id, String title, String details, int sortOrder, Instant createdAt) {
    }

    /** Full desired order of the user's strategy ids. Must contain each of the user's strategies exactly once. */
    public record ReorderRequest(@NotEmpty @Size(max = 100) List<UUID> ids) {
    }

    public record BreathingPattern(String id, String name, String description, int inhaleSeconds, int holdSeconds,
                                   int exhaleSeconds, int holdAfterExhaleSeconds, int defaultCycles) {
    }

    public record GroundingStep(int count, String sense, String prompt) {
    }

    public record ToolkitContent(List<BreathingPattern> breathing, List<GroundingStep> grounding,
                                 List<String> reflectionPrompts, List<String> gratitudePrompts, String disclaimer) {
    }
}
