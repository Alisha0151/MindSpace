package com.mindspace.privacy;

import com.mindspace.checkin.CheckInDtos.CheckInResponse;
import com.mindspace.habit.domain.Cadence;
import com.mindspace.journal.JournalDtos.JournalDetail;
import com.mindspace.user.Theme;
import com.mindspace.wellness.WellnessDtos.CopingResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PrivacyDtos {

    private PrivacyDtos() {
    }

    public static final int SCHEMA_VERSION = 1;

    public record ExportProfile(UUID id, String email, String displayName, String timezone, Theme theme, Instant createdAt) {
    }

    public record ExportConsent(String type, boolean granted, String policyVersion, Instant recordedAt) {
    }

    public record ExportPause(LocalDate pausedOn, LocalDate resumedOn) {
    }

    public record ExportHabit(UUID id, String name, String description, Cadence cadence, int targetCount,
                              LocalDate startDate, LocalDate endDate, List<ExportPause> pauses,
                              List<LocalDate> completions) {
    }

    /** Everything MindSpace holds about the user. Contains no password hash, token hashes or other users' data. */
    public record ExportDocument(int schemaVersion, Instant exportedAt, ExportProfile profile,
                                 List<ExportConsent> consents, List<CheckInResponse> checkIns,
                                 List<JournalDetail> journalEntries, List<ExportHabit> habits,
                                 List<CopingResponse> copingStrategies) {
    }

    public record DeleteAccountRequest(@NotBlank @Size(max = 200) String password) {
    }
}
