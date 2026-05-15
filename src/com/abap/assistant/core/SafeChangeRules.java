package com.abap.assistant.core;

import java.time.LocalDate;

public final class SafeChangeRules {
    public static final String PRODUCT_NAME = "ABAP Eclipse Assistant";
    public static final String COMMENT_PREFIX = "\"*";
    public static final String MANUAL_REVIEW_REQUIRED = "manual review required";
    public static final String ORIGINAL_CODE_RETAINED = "Original code should be retained/commented below when applying manually";

    public String header(String reason, LocalDate date) {
        LocalDate effectiveDate = date == null ? LocalDate.now() : date;
        String safeReason = reason == null || reason.isBlank() ? "TODO/TBC" : reason.strip();
        return String.join(System.lineSeparator(),
            COMMENT_PREFIX + " Proposed by " + PRODUCT_NAME + " - " + MANUAL_REVIEW_REQUIRED,
            COMMENT_PREFIX + " " + ORIGINAL_CODE_RETAINED,
            COMMENT_PREFIX + " Date: " + effectiveDate,
            COMMENT_PREFIX + " Change reason: " + safeReason,
            "");
    }
}
