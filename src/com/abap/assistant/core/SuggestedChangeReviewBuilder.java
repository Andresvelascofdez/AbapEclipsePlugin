package com.abap.assistant.core;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SuggestedChangeReviewBuilder {
    private static final int MAX_EXCERPT_CHARS = 3000;
    private static final int MAX_EXPLANATION_CHARS = 1200;
    private static final Pattern FENCED_ABAP = Pattern.compile("(?is)```(?:abap)?\\s*(.*?)```");
    private final SafeChangeRules rules;
    private final LocalDate date;

    public SuggestedChangeReviewBuilder() {
        this(new SafeChangeRules(), LocalDate.now());
    }

    public SuggestedChangeReviewBuilder(SafeChangeRules rules, LocalDate date) {
        this.rules = rules == null ? new SafeChangeRules() : rules;
        this.date = date == null ? LocalDate.now() : date;
    }

    public SuggestedChangeReview build(String originalText, String responseText) {
        String suggestion = extractFirstCodeBlock(responseText);
        if (suggestion.isBlank()) {
            return new SuggestedChangeReview(excerpt(originalText), "", explanation(responseText), "", "");
        }

        String explanation = explanation(responseText);
        String riskNotes = "Copy-only suggestion. The plug-in does not write to SAP, activate objects or modify repositories.";
        String copyText = rules.header(explanation, date) + suggestion.strip();
        return new SuggestedChangeReview(excerpt(originalText), suggestion, explanation, riskNotes, copyText);
    }

    private static String extractFirstCodeBlock(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            return "";
        }
        Matcher matcher = FENCED_ABAP.matcher(responseText);
        return matcher.find() ? matcher.group(1).strip() : "";
    }

    private static String explanation(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            return "";
        }
        String withoutCode = FENCED_ABAP.matcher(responseText).replaceAll("").strip();
        if (withoutCode.length() <= MAX_EXPLANATION_CHARS) {
            return withoutCode;
        }
        return withoutCode.substring(0, MAX_EXPLANATION_CHARS).strip() + " ...";
    }

    private static String excerpt(String originalText) {
        if (originalText == null || originalText.isBlank()) {
            return "";
        }
        String stripped = originalText.strip();
        if (stripped.length() <= MAX_EXCERPT_CHARS) {
            return stripped;
        }
        return stripped.substring(0, MAX_EXCERPT_CHARS).strip() + System.lineSeparator() + "[Excerpt truncated locally.]";
    }
}
