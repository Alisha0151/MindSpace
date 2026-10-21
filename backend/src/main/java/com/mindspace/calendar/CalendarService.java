package com.mindspace.calendar;

import com.mindspace.calendar.CalendarDtos.CalendarDay;
import com.mindspace.calendar.CalendarDtos.CalendarMonth;
import com.mindspace.checkin.CheckInRepository;
import com.mindspace.checkin.MoodCheckIn;
import com.mindspace.checkin.TagType;
import com.mindspace.checkin.CheckInTag;
import com.mindspace.common.DateRules;
import com.mindspace.common.FieldViolation;
import com.mindspace.common.Text;
import com.mindspace.common.ValidationException;
import com.mindspace.journal.JournalRepository;
import com.mindspace.journal.JournalRepository.DayCount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * A month grid of check-in and journal activity. Only days with something in them are returned.
 * Optional filters apply to check-ins; journal counts are shown for days that pass (or all days when no
 * filter is set).
 */
@Service
public class CalendarService {

    private final CheckInRepository checkIns;
    private final JournalRepository journal;

    public CalendarService(CheckInRepository checkIns, JournalRepository journal) {
        this.checkIns = checkIns;
        this.journal = journal;
    }

    @Transactional(readOnly = true)
    public CalendarMonth month(UUID userId, String month, Integer minMood, Integer maxMood, String emotion, String activity) {
        YearMonth ym = parse(month);
        List<FieldViolation> problems = new ArrayList<>();
        if (minMood != null && (minMood < 1 || minMood > 5)) {
            problems.add(new FieldViolation("minMood", "must be between 1 and 5"));
        }
        if (maxMood != null && (maxMood < 1 || maxMood > 5)) {
            problems.add(new FieldViolation("maxMood", "must be between 1 and 5"));
        }
        if (minMood != null && maxMood != null && minMood > maxMood) {
            problems.add(new FieldViolation("minMood", "must not exceed maxMood"));
        }
        ValidationException.throwIfAny(problems);

        boolean filtered = minMood != null || maxMood != null || !Text.isBlank(emotion) || !Text.isBlank(activity);
        String emo = Text.isBlank(emotion) ? null : Text.trim(emotion).toLowerCase();
        String act = Text.isBlank(activity) ? null : Text.trim(activity).toLowerCase();

        var from = ym.atDay(1);
        var to = ym.atEndOfMonth();
        Map<java.time.LocalDate, Long> journalCounts = new HashMap<>();
        for (DayCount d : journal.countByDay(userId, from, to)) {
            journalCounts.put(d.getDate(), d.getCount());
        }
        Map<java.time.LocalDate, CalendarDay> days = new HashMap<>();
        for (MoodCheckIn c : checkIns.findByUserIdAndLocalDateBetweenOrderByLocalDateAsc(userId, from, to)) {
            if (minMood != null && c.getMoodScore() < minMood) continue;
            if (maxMood != null && c.getMoodScore() > maxMood) continue;
            List<String> emotions = labels(c, TagType.EMOTION);
            List<String> activities = labels(c, TagType.ACTIVITY);
            if (emo != null && emotions.stream().noneMatch(l -> l.equalsIgnoreCase(emo))) continue;
            if (act != null && activities.stream().noneMatch(l -> l.equalsIgnoreCase(act))) continue;
            days.put(c.getLocalDate(), new CalendarDay(c.getLocalDate(), c.getId(), c.getMoodScore(), emotions, activities,
                    journalCounts.getOrDefault(c.getLocalDate(), 0L).intValue()));
        }
        if (!filtered) {
            for (Map.Entry<java.time.LocalDate, Long> e : journalCounts.entrySet()) {
                days.computeIfAbsent(e.getKey(), k -> new CalendarDay(k, null, null, List.of(), List.of(), e.getValue().intValue()));
            }
        }
        List<CalendarDay> ordered = new ArrayList<>(days.values());
        ordered.sort(Comparator.comparing(CalendarDay::date));
        return new CalendarMonth(ym.toString(), from, to, ordered);
    }

    private static YearMonth parse(String month) {
        if (Text.isBlank(month)) {
            throw new ValidationException("month", "is required (format YYYY-MM)");
        }
        try {
            YearMonth ym = YearMonth.parse(month.trim());
            if (ym.getYear() < 2000 || ym.getYear() > 2100) {
                throw new ValidationException("month", "year must be between 2000 and 2100");
            }
            return ym;
        } catch (DateTimeException e) {
            throw new ValidationException("month", "must use the format YYYY-MM");
        }
    }

    private static List<String> labels(MoodCheckIn c, TagType type) {
        return new TreeSet<>(c.getTags().stream().filter(t -> t.getTagType() == type).map(CheckInTag::getLabel).toList())
                .stream().toList();
    }
}
