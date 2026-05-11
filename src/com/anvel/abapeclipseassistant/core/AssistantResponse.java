package com.anvel.abapeclipseassistant.core;

public record AssistantResponse(String text, PrivacyScope privacyScope, boolean inputWasRedacted) {
    public AssistantResponse {
        text = text == null ? "" : text.strip();
        privacyScope = privacyScope == null ? PrivacyScope.MIXED_OR_UNKNOWN : privacyScope;
    }
}

