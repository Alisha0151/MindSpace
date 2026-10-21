package com.mindspace.checkin.domain;

import com.mindspace.common.FieldViolation;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation for a mood check-in. Mirrors the CHECK constraints in {@code V2__mood_check_ins.sql};
 * the database remains the last line of defence.
 *
 * <p>Score meanings are self-reported observations, not clinical measures:
 * mood 1 = very low .. 5 = very good; stress 1 = very calm .. 5 = very stressed;
 * energy 1 = very low .. 5 = very high; sleep quality 1 = very poor .. 5 = very good.
 */
public final class CheckInRules {

    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 5;
    public static final int MAX_SLEEP_MINUTES = 24 * 60;
    public static final int MAX_NOTES_LENGTH = 2000;

    private CheckInRules() {
    }

    public static List<FieldViolation> validate(Integer mood, Integer stress, Integer energy,
                                                Integer sleepMinutes, Integer sleepQuality, String notes) {
        List<FieldViolation> problems = new ArrayList<>();
        requireScore(problems, "moodScore", mood);
        requireScore(problems, "stressScore", stress);
        requireScore(problems, "energyScore", energy);

        if (sleepMinutes == null) {
            problems.add(new FieldViolation("sleepMinutes", "is required"));
        } else if (sleepMinutes < 0 || sleepMinutes > MAX_SLEEP_MINUTES) {
            problems.add(new FieldViolation("sleepMinutes", "must be between 0 and " + MAX_SLEEP_MINUTES));
        }

        if (sleepQuality != null && (sleepQuality < MIN_SCORE || sleepQuality > MAX_SCORE)) {
            problems.add(new FieldViolation("sleepQuality", scoreRangeMessage()));
        }

        if (notes != null && notes.codePointCount(0, notes.length()) > MAX_NOTES_LENGTH) {
            problems.add(new FieldViolation("notes", "must be at most " + MAX_NOTES_LENGTH + " characters"));
        }
        return problems;
    }

    private static void requireScore(List<FieldViolation> problems, String field, Integer value) {
        if (value == null) {
            problems.add(new FieldViolation(field, "is required"));
        } else if (value < MIN_SCORE || value > MAX_SCORE) {
            problems.add(new FieldViolation(field, scoreRangeMessage()));
        }
    }

    private static String scoreRangeMessage() {
        return "must be between " + MIN_SCORE + " and " + MAX_SCORE;
    }
}
