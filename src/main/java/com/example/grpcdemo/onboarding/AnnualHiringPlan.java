package com.example.grpcdemo.onboarding;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Enumerates the selectable annual hiring plan buckets during onboarding.
 */
public enum AnnualHiringPlan {
    ONE_TO_TEN("1-10人"),
    TEN_TO_FIFTY("10-50人"),
    FIFTY_TO_ONE_HUNDRED("50-100人"),
    ONE_HUNDRED_TO_TWO_HUNDRED("100-200人"),
    TWO_HUNDRED_TO_THREE_HUNDRED("200-300人"),
    MORE_THAN_THREE_HUNDRED("300人以上");

    private final String label;

    AnnualHiringPlan(String label) {
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
    public static AnnualHiringPlan fromValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return Arrays.stream(values())
                .filter(plan -> plan.name().equalsIgnoreCase(trimmed) || plan.label.equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无法识别的年度招聘计划: " + value));
    }
}
