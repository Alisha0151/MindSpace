package com.mindspace.analytics.domain;

import com.mindspace.analytics.domain.MoodAnalytics.Coverage;
import com.mindspace.analytics.domain.MoodAnalytics.HabitMoodComparison;
import com.mindspace.analytics.domain.MoodAnalytics.LabelCount;
import com.mindspace.analytics.domain.MoodAnalytics.RangeComparison;
import com.mindspace.analytics.domain.MoodAnalytics.SleepBucket;
import com.mindspace.analytics.domain.MoodAnalytics.SleepMoodBucket;
import com.mindspace.analytics.domain.MoodAnalytics.TrendBucket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static com.mindspace.analytics.domain.Fixtures.oct;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoodAnalyticsTest {

    private static BigDecimal d(String value) {
        return new BigDecimal(value);
    }

    @Test
    @DisplayName("weekly trend equals the SQL-verified averages for the two ISO weeks")
    void weeklyTrend() {
        List<TrendBucket> weeks = MoodAnalytics.weeklyTrend(Fixtures.userA());
        assertEquals(2, weeks.size());
        assertEquals(new TrendBucket(oct(5), 5, d("3.40"), d("2.60"), d("3.40"), d("408")), weeks.get(0));
        assertEquals(new TrendBucket(oct(12), 5, d("3.20"), d("2.80"), d("3.20"), d("412")), weeks.get(1));
    }

    @Test
    @DisplayName("monthly trend equals the SQL-verified October averages")
    void monthlyTrend() {
        List<TrendBucket> months = MoodAnalytics.monthlyTrend(Fixtures.userA());
        assertEquals(1, months.size());
        assertEquals(new TrendBucket(oct(1), 10, d("3.30"), d("2.70"), d("3.30"), d("410")), months.get(0));
    }

    @Test
    @DisplayName("a Sunday and the following Monday fall into different weeks")
    void weekBoundary() {
        List<CheckInPoint> points = List.of(
                new CheckInPoint(oct(11), 3, 3, 3, 400, Set.of()),   // Sunday
                new CheckInPoint(oct(12), 5, 3, 3, 400, Set.of()));  // Monday
        List<TrendBucket> weeks = MoodAnalytics.weeklyTrend(points);
        assertEquals(2, weeks.size());
        assertEquals(oct(5), weeks.get(0).periodStart());
        assertEquals(oct(12), weeks.get(1).periodStart());
    }

    @Test
    @DisplayName("mood distribution is zero-filled")
    void distribution() {
        assertEquals(List.of(1, 2, 2, 3, 2), MoodAnalytics.moodDistribution(Fixtures.userA()));
        assertEquals(List.of(0, 0, 0, 0, 0), MoodAnalytics.moodDistribution(List.of()));
    }

    @Test
    @DisplayName("top emotions: count descending, ties alphabetical")
    void topEmotions() {
        assertEquals(List.of(new LabelCount("calm", 6), new LabelCount("hopeful", 3), new LabelCount("anxious", 2)),
                MoodAnalytics.topEmotions(Fixtures.userA(), 3));
        assertEquals(List.of(), MoodAnalytics.topEmotions(Fixtures.userA(), 0));
    }

    @Test
    @DisplayName("sleep buckets: counts and average mood match SQL")
    void sleepBuckets() {
        assertEquals(List.of(
                        new SleepMoodBucket(SleepBucket.UNDER_6H, 2, d("1.50")),
                        new SleepMoodBucket(SleepBucket.SIX_TO_8H, 6, d("3.33")),
                        new SleepMoodBucket(SleepBucket.EIGHT_PLUS, 2, d("5.00"))),
                MoodAnalytics.sleepVersusMood(Fixtures.userA()));
    }

    @Test
    @DisplayName("sleep bucket edges: 359 is <6h, 360 is 6-8h, 479 is 6-8h, 480 is 8h+")
    void sleepBucketEdges() {
        assertEquals(SleepBucket.UNDER_6H, SleepBucket.of(359));
        assertEquals(SleepBucket.SIX_TO_8H, SleepBucket.of(360));
        assertEquals(SleepBucket.SIX_TO_8H, SleepBucket.of(479));
        assertEquals(SleepBucket.EIGHT_PLUS, SleepBucket.of(480));
    }

    @Test
    @DisplayName("habit vs mood: 6 done days avg 4.17, 4 other days avg 2.00; extra completions ignored")
    void habitVersusMood() {
        HabitMoodComparison result = MoodAnalytics.habitVersusMood(Fixtures.userA(), Fixtures.eveningWalk());
        assertEquals(new HabitMoodComparison(6, d("4.17"), 4, d("2.00")), result);
    }

    @Test
    @DisplayName("coverage: 10 of 14 days = 71.4%")
    void coverage() {
        assertEquals(new Coverage(10, 14, d("71.4")), MoodAnalytics.coverage(Fixtures.userA(), oct(5), oct(18)));
    }

    @Test
    @DisplayName("coverage ignores check-ins outside the range and counts a day once")
    void coverageRangeAndDistinct() {
        Coverage c = MoodAnalytics.coverage(Fixtures.userA(), oct(5), oct(9));
        assertEquals(new Coverage(5, 5, d("100.0")), c);
    }

    @Test
    @DisplayName("range comparison: 3.40 vs 3.20")
    void compareRanges() {
        RangeComparison r = MoodAnalytics.compareRanges(Fixtures.userA(), oct(5), oct(11), oct(12), oct(18));
        assertEquals(new RangeComparison(5, d("3.40"), 5, d("3.20")), r);
    }

    @Test
    @DisplayName("empty data: no exceptions, null averages, zero coverage, empty trends")
    void emptyData() {
        assertEquals(List.of(), MoodAnalytics.weeklyTrend(List.of()));
        assertEquals(List.of(), MoodAnalytics.monthlyTrend(List.of()));
        assertEquals(List.of(), MoodAnalytics.sleepVersusMood(List.of()));
        assertEquals(new Coverage(0, 14, d("0.0")), MoodAnalytics.coverage(List.of(), oct(5), oct(18)));
        RangeComparison r = MoodAnalytics.compareRanges(List.of(), oct(5), oct(11), oct(12), oct(18));
        assertNull(r.avgMood1());
        assertNull(r.avgMood2());
        HabitMoodComparison h = MoodAnalytics.habitVersusMood(List.of(), Set.of());
        assertNull(h.avgMoodDone());
        assertNull(h.avgMoodNotDone());
    }

    @Test
    @DisplayName("invalid ranges are rejected")
    void invalidRange() {
        assertThrows(IllegalArgumentException.class, () -> MoodAnalytics.coverage(List.of(), oct(18), oct(5)));
    }

    @Test
    @DisplayName("rounding is HALF_UP: 2.125 mood-average case rounds to 2.13")
    void rounding() {
        // moods 2,2,2,2,2,2,2,3 -> 17/8 = 2.125 -> 2.13 (HALF_UP)
        List<CheckInPoint> pts = List.of(
                new CheckInPoint(oct(5), 2, 3, 3, 400, Set.of()), new CheckInPoint(oct(6), 2, 3, 3, 400, Set.of()),
                new CheckInPoint(oct(7), 2, 3, 3, 400, Set.of()), new CheckInPoint(oct(8), 2, 3, 3, 400, Set.of()),
                new CheckInPoint(oct(9), 2, 3, 3, 400, Set.of()), new CheckInPoint(oct(10), 2, 3, 3, 400, Set.of()),
                new CheckInPoint(oct(11), 2, 3, 3, 400, Set.of()), new CheckInPoint(oct(4), 3, 3, 3, 400, Set.of()));
        RangeComparison r = MoodAnalytics.compareRanges(pts, oct(1), oct(31), oct(1), oct(2));
        assertEquals(d("2.13"), r.avgMood1());
    }

    @Test
    @DisplayName("enough-data threshold")
    void enoughData() {
        assertFalse(MoodAnalytics.hasEnoughData(4));
        assertTrue(MoodAnalytics.hasEnoughData(5));
    }
}
