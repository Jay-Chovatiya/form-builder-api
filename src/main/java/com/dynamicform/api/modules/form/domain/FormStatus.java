package com.dynamicform.api.modules.form.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FormStatus {
    DRAFT("Draft"),
    PUBLISHED("Published"),
    UNPUBLISHED("Unpublished"),
    ARCHIVED("Archived");

    private final String value;

    FormStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static FormStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DRAFT;
        }
        for (FormStatus status : FormStatus.values()) {
            if (status.value.equalsIgnoreCase(value.trim()) || status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown form status: " + value);
    }
}
