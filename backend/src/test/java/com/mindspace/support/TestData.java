package com.mindspace.support;

import java.util.UUID;

/** Synthetic data only. */
public final class TestData {

    private TestData() {
    }

    public static final String PASSWORD = "correct horse battery staple 42";

    public static String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
    }

    public static String registerJson(String email) {
        return """
                {"email":"%s","password":"%s","confirmPassword":"%s","displayName":"Test Person","timezone":"Asia/Kolkata","acceptTerms":true}
                """.formatted(email, PASSWORD, PASSWORD);
    }

    public static String checkInJson(int mood) {
        return """
                {"moodScore":%d,"stressScore":4,"energyScore":3,"sleepMinutes":420,"sleepQuality":3,
                 "emotions":["calm"],"activities":["walk"],"contexts":[],"notes":"synthetic note"}
                """.formatted(mood);
    }
}
