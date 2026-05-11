package com.anvel.abapeclipseassistant.core;

public record AssistantRequest(AssistantMode mode, String userText, String selectedCode) {
    public AssistantRequest {
        mode = mode == null ? AssistantMode.GENERAL_HELP : mode;
        userText = normalize(userText);
        selectedCode = normalize(selectedCode);
        if (userText.isBlank() && selectedCode.isBlank()) {
            throw new IllegalArgumentException("A question or selected ABAP code is required.");
        }
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

