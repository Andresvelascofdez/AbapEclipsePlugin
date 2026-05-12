package com.abap.assistant.cli;

import com.abap.assistant.core.AbapContextClassifier;
import com.abap.assistant.core.AssistantMode;
import com.abap.assistant.core.AssistantPromptBuilder;
import com.abap.assistant.core.AssistantRequest;
import com.abap.assistant.core.AssistantResponse;
import com.abap.assistant.core.AssistantService;
import com.abap.assistant.core.OpenAiResponsesClient;
import com.abap.assistant.core.OpenAiSettings;
import com.abap.assistant.core.SensitiveDataRedactor;

public final class AssistantCli {
    private AssistantCli() {
    }

    public static void main(String[] args) throws Exception {
        String prompt = args.length == 0
            ? "Explain the purpose of SELECT SINGLE in SAP ABAP using public SAP knowledge only."
            : String.join(" ", args);

        AssistantService service = new AssistantService(
            new AssistantPromptBuilder(new SensitiveDataRedactor(), new AbapContextClassifier()),
            new OpenAiResponsesClient(OpenAiSettings.fromEnvironment()));
        AssistantResponse response = service.answer(new AssistantRequest(AssistantMode.GENERAL_HELP, prompt, ""));

        System.out.println("Privacy scope: " + response.privacyScope());
        System.out.println(response.text());
    }
}

