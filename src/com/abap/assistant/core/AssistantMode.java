package com.abap.assistant.core;

public enum AssistantMode {
    FREE_CHAT("Free chat with editor context"),
    EXPLAIN_SELECTION("Explain selected ABAP code"),
    FIND_DEFECT("Find possible defects"),
    SUGGEST_TESTS("Suggest ABAP Unit or manual tests"),
    REFACTORING_IDEAS("Suggest safe refactoring ideas"),
    GENERAL_HELP("Answer an ABAP or ADT question");

    private final String label;

    AssistantMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
