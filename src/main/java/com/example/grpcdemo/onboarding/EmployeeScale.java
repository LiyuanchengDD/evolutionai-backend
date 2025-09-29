package com.example.grpcdemo.onboarding;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Enumerates the selectable company size ranges used during enterprise onboarding.
 */
public enum EmployeeScale {
    LESS_THAN_TEN("10人以内"),
    TEN_TO_FIFTY("10-50人"),
    FIFTY_TO_TWO_HUNDRED("50-200人"),
    TWO_HUNDRED_TO_FIVE_HUNDRED("200-500人"),
    FIVE_HUNDRED_TO_ONE_THOUSAND("500-1000人"),
    ONE_THOUSAND_TO_FIVE_THOUSAND("1000-5000人"),
    MORE_THAN_FIVE_THOUSAND("5000人以上");

    private final String label;

    EmployeeScale(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @JsonValue
    public String getCode() {
        return name();
    }

    @JsonCreator
    public static EmployeeScale fromValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return Arrays.stream(values())
                .filter(scale -> scale.name().equalsIgnoreCase(trimmed) || scale.label.equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无法识别的企业规模: " + value));
    }
}
