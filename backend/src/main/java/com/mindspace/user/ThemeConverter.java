package com.mindspace.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class ThemeConverter implements AttributeConverter<Theme, String> {

    @Override
    public String convertToDatabaseColumn(Theme attribute) {
        return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Theme convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Theme.valueOf(dbData.toUpperCase(Locale.ROOT));
    }
}
