package com.abap.assistant.core;

public final class SuggestedChangeReview {
    private final String originalExcerpt;
    private final String suggestedBlock;
    private final String explanation;
    private final String riskNotes;
    private final String copyText;

    public SuggestedChangeReview(
        String originalExcerpt,
        String suggestedBlock,
        String explanation,
        String riskNotes,
        String copyText) {
        this.originalExcerpt = originalExcerpt == null ? "" : originalExcerpt.strip();
        this.suggestedBlock = suggestedBlock == null ? "" : suggestedBlock.strip();
        this.explanation = explanation == null ? "" : explanation.strip();
        this.riskNotes = riskNotes == null ? "" : riskNotes.strip();
        this.copyText = copyText == null ? "" : copyText.strip();
    }

    public String originalExcerpt() {
        return originalExcerpt;
    }

    public String suggestedBlock() {
        return suggestedBlock;
    }

    public String explanation() {
        return explanation;
    }

    public String riskNotes() {
        return riskNotes;
    }

    public String copyText() {
        return copyText;
    }

    public boolean hasSuggestion() {
        return !suggestedBlock.isBlank();
    }

    public String displayText() {
        if (!hasSuggestion()) {
            return "Suggested change review: no fenced ABAP code suggestion detected.";
        }
        return String.join(System.lineSeparator(),
            "Suggested change review",
            "",
            "Original excerpt:",
            originalExcerpt.isBlank() ? "(none available)" : originalExcerpt,
            "",
            "Suggested block:",
            copyText,
            "",
            "Explanation:",
            explanation.isBlank() ? "(none provided)" : explanation,
            "",
            "Risk notes:",
            riskNotes.isBlank() ? "Manual review required before any SAP change." : riskNotes);
    }
}
