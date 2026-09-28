package com.electrician.tracker.domain.converter;

import java.time.LocalDateTime;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores {@link LocalDateTime} fields as ISO-8601 TEXT (yyyy-MM-ddTHH:mm:ss).
 */
@Converter(autoApply = true)
public class LocalDateTimeToTextConverter implements AttributeConverter<LocalDateTime, String> {

    @Override
    public String convertToDatabaseColumn(LocalDateTime attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String dbData) {
        return dbData == null ? null : LocalDateTime.parse(dbData);
    }
}
