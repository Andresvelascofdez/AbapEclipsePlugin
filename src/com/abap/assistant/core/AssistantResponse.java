package com.abap.assistant.core;

public final class AssistantResponse {
    private final String text;
    private final PrivacyScope privacyScope;
    private final boolean inputWasRedacted;

    public AssistantResponse(String text, PrivacyScope privacyScope, boolean inputWasRedacted) {
        this.text = text == null ? "" : text.strip();
        this.privacyScope = privacyScope == null ? PrivacyScope.MIXED_OR_UNKNOWN : privacyScope;
        this.inputWasRedacted = inputWasRedacted;
    }

    public String text() {
        return text;
    }

    public PrivacyScope privacyScope() {
        return privacyScope;
    }

    public boolean inputWasRedacted() {
        return inputWasRedacted;
    }
}
