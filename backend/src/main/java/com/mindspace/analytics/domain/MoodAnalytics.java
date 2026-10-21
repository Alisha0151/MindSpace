package com.mindspace.analytics.domain;

import com.mindspace.common.DateRules;
import com.mindspace.common.Numbers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Personal-observation analytics over a user's check-ins. Results describe what the user recorded;
 * they are correlations/observations, never causal claims.
 *
 * <p>Rules: averages are rounded HALF_UP (mood/stress/energy to 2 places, sleep minutes to whole
 * minutes, percentages to 1 place); an empty input yields empty lists or {@code null} averages,
 * never an exception from division by zero; weeks are Monday-Sunday.
 */
public final class MoodAnalytics {

    /** Minimum observations before the UI should present a comparison as meaningful. */
    public static final int MIN_OBSERVATIONS_FOR_COMPARISON = 5;

    private MoodAnalytics() {
    }

    public record TrendBucket(LocalDate periodStart, int observations, BigDecimal avgMood,
                              BigDecimal avgStress, BigDecimal avgEnergy, BigDecimal avgSleepMinutes) {
    }

    public record LabelCount(String label, int count) {
    }

    public enum SleepBucket {
        UNDER_6H("<6h"), SIX_TO_8H("6-8h"), EIGHT_PLUS("8h+");

        private final String label;

        SleepBucket(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static SleepBucket of(int sleepMinutes) {
            if (sleepMinutes < 360) {
                return UNDER_6H;
            }
            return sleepMinutes < 480 ? SIX_TO_8H : EIGHT_PLUS;
        }
    }

    public record SleepMoodBucket(SleepBucket bucket, int observations, BigDecimal avgMood) {
    }

    /** Mood on days with a completed habit versus days without; averages are null for an empty group. */
    public record HabitMoodComparison(int doneDays, BigDecimal avgMoodDone,
                                      int notDoneDays, BigDecimal avgMoodNotDone) {
    }

    public record Coverage(int daysLogged, long daysInRange, BigDecimal percent) {
    }

    public record RangeComparison(int observations1, BigDecimal avgMood1,
                                  int observations2, BigDecimal avgMood2) {
    }

    public static List<TrendBucket> weeklyTrend(Collection<CheckInPoint> points) {
        return trend(points, DateRules::weekStart);
    }

    public static List<TrendBucket> monthlyTrend(Collection<CheckInPoint> points) {
        return trend(points, d -> d.withDayOfMonth(1));
    }

    /** Counts for mood scores 1..5, in that order, zero-filled. */
    public static List<Integer> moodDistribution(Collection<CheckInPoint> points) {
        int[] counts = new int[5];
        for (CheckInPoint p : points) {
            counts[p.mood() - 1]++;
        }
        List<Integer> result = new ArrayList<>(5);
        for (int count : counts) {
            result.add(count);
        }
        return result;
    }

    /** Most frequently selected emotions: count descending, then label ascending. */
    public static List<LabelCount> topEmotions(Collection<CheckInPoint> points, int limit) {
        Map<String, Integer> counts = new HashMap<>();
        for (CheckInPoint p : points) {
            for (String emotion : p.emotions()) {
                counts.merge(emotion, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .map(e -> new LabelCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(LabelCount::count).reversed().thenComparing(LabelCount::label))
                .limit(Math.max(0, limit))
                .toList();
    }

    /** Non-empty sleep-duration buckets in ascending order, with the average mood recorded on those days. */
    public static List<SleepMoodBucket> sleepVersusMood(Collection<CheckInPoint> points) {
        Map<SleepBucket, List<CheckInPoint>> grouped = new TreeMap<>();
        for (CheckInPoint p : points) {
            grouped.computeIfAbsent(SleepBucket.of(p.sleepMinutes()), k -> new ArrayList<>()).add(p);
        }
        List<SleepMoodBucket> result = new ArrayList<>();
        grouped.forEach((bucket, list) ->
                result.add(new SleepMoodBucket(bucket, list.size(), averageMood(list))));
        return result;
    }

    /**
     * Splits the check-in days by whether the habit was completed that same day.
     * Completions on days without a check-in are ignored.
     */
    public static HabitMoodComparison habitVersusMood(Collection<CheckInPoint> points, Set<LocalDate> completedDates) {
        List<CheckInPoint> done = new ArrayList<>();
        List<CheckInPoint> notDone = new ArrayList<>();
        for (CheckInPoint p : points) {
            (completedDates.contains(p.date()) ? done : notDone).add(p);
        }
        return new HabitMoodComparison(done.size(), averageMood(done), notDone.size(), averageMood(notDone));
    }

    /** Distinct logged days in {@code [from, to]} against the number of days in that range. */
    public static Coverage coverage(Collection<CheckInPoint> points, LocalDate from, LocalDate to) {
        long days = DateRules.daysInclusive(from, to);
        int logged = (int) points.stream()
                .map(CheckInPoint::date)
                .filter(d -> DateRules.inRange(d, from, to))
                .distinct()
                .count();
        return new Coverage(logged, days, Numbers.percent(logged, days, 1));
    }

    /** Average mood in two user-chosen inclusive ranges. */
    public static RangeComparison compareRanges(Collection<CheckInPoint> points,
                                                LocalDate from1, LocalDate to1,
                                                LocalDate from2, LocalDate to2) {
        DateRules.daysInclusive(from1, to1);
        DateRules.daysInclusive(from2, to2);
        List<CheckInPoint> first = points.stream().filter(p -> DateRules.inRange(p.date(), from1, to1)).toList();
        List<CheckInPoint> second = points.stream().filter(p -> DateRules.inRange(p.date(), from2, to2)).toList();
        return new RangeComparison(first.size(), averageMood(first), second.size(), averageMood(second));
    }

    public static boolean hasEnoughData(int observations) {
        return observations >= MIN_OBSERVATIONS_FOR_COMPARISON;
    }

    private static List<TrendBucket> trend(Collection<CheckInPoint> points, Function<LocalDate, LocalDate> periodOf) {
        Map<LocalDate, List<CheckInPoint>> grouped = new TreeMap<>();
        for (CheckInPoint p : points) {
            grouped.computeIfAbsent(periodOf.apply(p.date()), k -> new ArrayList<>()).add(p);
        }
        List<TrendBucket> buckets = new ArrayList<>();
        grouped.forEach((start, list) -> {
            long n = list.size();
            long mood = 0;
            long stress = 0;
            long energy = 0;
            long sleep = 0;
            for (CheckInPoint p : list) {
                mood += p.mood();
                stress += p.stress();
                energy += p.energy();
                sleep += p.sleepMinutes();
            }
            buckets.add(new TrendBucket(start, list.size(),
                    Numbers.average(mood, n, 2), Numbers.average(stress, n, 2),
                    Numbers.average(energy, n, 2), Numbers.average(sleep, n, 0)));
        });
        return buckets;
    }

    private static BigDecimal averageMood(List<CheckInPoint> list) {
        long sum = 0;
        for (CheckInPoint p : list) {
            sum += p.mood();
        }
        return Numbers.average(sum, list.size(), 2);
    }
}
