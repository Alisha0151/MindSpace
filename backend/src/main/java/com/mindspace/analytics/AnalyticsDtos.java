package com.mindspace.analytics;

import com.mindspace.analytics.domain.MoodAnalytics.Coverage;
import com.mindspace.analytics.domain.MoodAnalytics.HabitMoodComparison;
import com.mindspace.analytics.domain.MoodAnalytics.LabelCount;
import com.mindspace.analytics.domain.MoodAnalytics.RangeComparison;
import com.mindspace.analytics.domain.MoodAnalytics.SleepMoodBucket;
import com.mindspace.analytics.domain.MoodAnalytics.TrendBucket;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record DailyPoint(LocalDate date, int mood, int stress, int energy, int sleepMinutes) {
    }

    /**
     * Every figure is an observed summary of the user's own entries, not a diagnosis or a causal claim.
     * {@code enoughData} is false below {@link com.mindspace.analytics.domain.MoodAnalytics#MIN_OBSERVATIONS_FOR_COMPARISON}
     * observations; the UI should then show "not enough data yet" instead of a pattern.
     */
    public record AnalyticsSummary(LocalDate from, LocalDate to, int observations, boolean enoughData,
                                   BigDecimal averageMood, List<DailyPoint> daily, List<TrendBucket> weeklyTrend,
                                   List<TrendBucket> monthlyTrend, List<Integer> moodDistribution,
                                   List<LabelCount> topEmotions, List<SleepMoodBucket> sleepVersusMood,
                                   Coverage coverage, UUID habitId, HabitMoodComparison habitVersusMood,
                                   RangeComparison comparison, LocalDate compareFrom, LocalDate compareTo) {
    }
}
