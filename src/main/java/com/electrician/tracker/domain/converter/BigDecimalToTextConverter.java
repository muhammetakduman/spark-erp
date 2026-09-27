package com.electrician.tracker.domain.converter;

import java.math.BigDecimal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores {@link BigDecimal} money/quantity fields as plain TEXT so SQLite
 * never rounds them through its native REAL (floating point) type.
 */
@Converter(autoApply = true)
public class BigDecimalToTextConverter implements AttributeConverter<BigDecimal, String> {

    @Override
    public String convertToDatabaseColumn(BigDecimal attribute) {
        return attribute == null ? null : attribute.toPlainString();
    }

    @Override
    public BigDecimal convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new BigDecimal(dbData);
    }
}
