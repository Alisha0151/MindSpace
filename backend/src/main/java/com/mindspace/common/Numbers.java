package com.mindspace.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Averages and percentages with one explicit rounding rule: HALF_UP, no division by zero. */
public final class Numbers {

    private Numbers() {
    }

    /** {@code sum / count} rounded HALF_UP to {@code scale} places, or {@code null} when there are no observations. */
    public static BigDecimal average(long sum, long count, int scale) {
        if (count <= 0) {
            return null;
        }
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), scale, RoundingMode.HALF_UP);
    }

    /** {@code 100 * part / total} rounded HALF_UP to {@code scale} places; zero when {@code total} is zero. */
    public static BigDecimal percent(long part, long total, int scale) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(part * 100L)
                .divide(BigDecimal.valueOf(total), scale, RoundingMode.HALF_UP);
    }
}
