package com.mindspace.calendar;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CalendarDtos {

    private CalendarDtos() {
    }

    /** One calendar cell. Deliberately contains no notes or journal text. */
    public record CalendarDay(LocalDate date, UUID checkInId, Integer moodScore, List<String> emotions,
                              List<String> activities, int journalEntries) {
    }

    public record CalendarMonth(String month, LocalDate from, LocalDate to, List<CalendarDay> days) {
    }
}
