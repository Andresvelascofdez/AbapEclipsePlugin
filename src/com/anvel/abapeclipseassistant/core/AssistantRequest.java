package com.anvel.abapeclipseassistant.core;

public final class AssistantRequest {
    private final AssistantMode mode;
    private final String userText;
    private final String selectedCode;

    public AssistantRequest(AssistantMode mode, String userText, String selectedCode) {
        this.mode = mode == null ? AssistantMode.GENERAL_HELP : mode;
        this.userText = normalize(userText);
        this.selectedCode = normalize(selectedCode);
        if (this.userText.isBlank() && this.selectedCode.isBlank()) {
            throw new IllegalArgumentException("A question or selected ABAP code is required.");
        }
    }

    public AssistantMode mode() {
        return mode;
    }

    public String userText() {
        return userText;
    }

    public String selectedCode() {
        return selectedCode;
    }

    public String combinedInput() {
        if (selectedCode.isBlank()) {
            return userText;
        }
        if (userText.isBlank()) {
            return selectedCode;
        }
        return userText + System.lineSeparator() + selectedCode;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
