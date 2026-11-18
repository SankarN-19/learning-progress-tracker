package com.example.lpt.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the status as its human-readable label, matching schema.sql / the API. */
@Converter
public class AssignmentStatusConverter implements AttributeConverter<AssignmentStatus, String> {

    @Override
    public String convertToDatabaseColumn(AssignmentStatus attribute) {
        return attribute == null ? null : attribute.label();
    }

    @Override
    public AssignmentStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AssignmentStatus.fromLabel(dbData);
    }
}
