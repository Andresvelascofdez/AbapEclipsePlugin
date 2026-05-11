package com.anvel.abapeclipseassistant.cli;

import com.anvel.abapeclipseassistant.core.AbapContextClassifier;
import com.anvel.abapeclipseassistant.core.AssistantMode;
import com.anvel.abapeclipseassistant.core.AssistantPromptBuilder;
import com.anvel.abapeclipseassistant.core.AssistantRequest;
import com.anvel.abapeclipseassistant.core.AssistantResponse;
import com.anvel.abapeclipseassistant.core.AssistantService;
import com.anvel.abapeclipseassistant.core.OpenAiResponsesClient;
import com.anvel.abapeclipseassistant.core.OpenAiSettings;
import com.anvel.abapeclipseassistant.core.SensitiveDataRedactor;

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

