package com.example.academic_service.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/** A list of ids stored as "12,15" (null or "" = empty list). */
@Converter
public class IntListConverter implements AttributeConverter<List<Integer>, String> {
    @Override
    public String convertToDatabaseColumn(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return null;
        StringBuilder b = new StringBuilder();
        for (Integer id : ids) {
            if (id == null) continue;
            if (b.length() > 0) b.append(',');
            b.append(id);
        }
        return b.length() == 0 ? null : b.toString();
    }

    @Override
    public List<Integer> convertToEntityAttribute(String text) {
        List<Integer> ids = new ArrayList<>();
        if (text == null || text.isBlank()) return ids;
        for (String part : text.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) ids.add(Integer.valueOf(t));
        }
        return ids;
    }
}
