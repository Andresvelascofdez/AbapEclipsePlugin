package com.abap.assistant.core;

public final class AssistantService {
    private final AssistantPromptBuilder promptBuilder;
    private final AiClient aiClient;

    public AssistantService(AssistantPromptBuilder promptBuilder, AiClient aiClient) {
        this.promptBuilder = promptBuilder;
        this.aiClient = aiClient;
    }

    public AssistantResponse answer(AssistantRequest request) throws Exception {
        AssistantPromptBuilder.BuiltPrompt builtPrompt = promptBuilder.build(request);
        String answer = aiClient.complete(builtPrompt.prompt());
        return new AssistantResponse(answer, builtPrompt.privacyScope(), builtPrompt.inputWasRedacted());
    }
}

