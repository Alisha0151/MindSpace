package com.mindspace.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** Stored lower-case in user_preferences.theme (CHECK: light, dark, system). */
public enum Theme {
    LIGHT, DARK, SYSTEM;

    @JsonValue
    public String wire() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static Theme fromWire(String value) {
        return Theme.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
