package com.abap.assistant.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class AssistantCoreTest {
    public static void main(String[] args) throws Exception {
        redactorRemovesSensitiveValues();
        classifierSeparatesCustomAndStandardContext();
        promptBuilderAppliesProjectRules();
        dotenvLoaderParsesQuotedValues();
        extractorReadsResponsesOutputText();
        assistantServiceUsesRedactedPrompt();
        System.out.println("All core tests passed.");
    }

    private static void redactorRemovesSensitiveValues() {
        String fakeKey = "sk-" + "proj-FAKE_KEY_FOR_TESTING_ONLY";
        String input = "Ticket TCK12345, handover HND77777, invoice INV999, key " + fakeKey + ", user person@example.com, client 100.";
        String output = new SensitiveDataRedactor().redact(input);

        assertFalse(output.contains(fakeKey), "OpenAI-style keys must be redacted.");
        assertTrue(output.contains("TCKXXXXX"), "Ticket placeholders must be used.");
        assertTrue(output.contains("HNDXXXXX"), "Handover placeholders must be used.");
        assertTrue(output.contains("INVXXXXX"), "Invoice placeholders must be used.");
        assertTrue(output.contains("user@example.invalid"), "Emails must be anonymised.");
        assertTrue(output.contains("client CLIENT_A"), "Client numbers must be anonymised.");
    }

    private static void classifierSeparatesCustomAndStandardContext() {
        AbapContextClassifier classifier = new AbapContextClassifier();
        assertEquals(PrivacyScope.CLIENT_SPECIFIC_CUSTOM, classifier.classify("CLASS zcl_billing_helper DEFINITION."), "Z objects are custom.");
        assertEquals(PrivacyScope.SAP_STANDARD_PUBLIC, classifier.classify("Use BAPI_ISUACCOUNT_GETDETAIL with ADT."), "SAP hints are public standard context.");
        assertEquals(PrivacyScope.MIXED_OR_UNKNOWN, classifier.classify("zcl_helper calls BAPI_TRANSACTION_COMMIT."), "Custom plus standard is mixed.");
    }

    private static void promptBuilderAppliesProjectRules() {
        AssistantPromptBuilder builder = new AssistantPromptBuilder(new SensitiveDataRedactor(), new AbapContextClassifier());
        AssistantPromptBuilder.BuiltPrompt prompt = builder.build(new AssistantRequest(
            AssistantMode.EXPLAIN_SELECTION,
            "Explain TCK12345",
            "SELECT * FROM zclient_table INTO TABLE @DATA(result)."));

        assertTrue(prompt.prompt().contains("Keep SAP standard/public knowledge separate"), "Prompt must keep public and private knowledge separate.");
        assertTrue(prompt.prompt().contains("TCKXXXXX"), "Prompt must contain anonymised ticket placeholders.");
        assertFalse(prompt.prompt().contains("TCK12345"), "Prompt must not contain raw tickets.");
        assertEquals(PrivacyScope.CLIENT_SPECIFIC_CUSTOM, prompt.privacyScope(), "Prompt must classify custom context.");
        assertTrue(prompt.inputWasRedacted(), "Prompt must report redaction.");
    }

    private static void dotenvLoaderParsesQuotedValues() throws Exception {
        Path temp = Files.createTempFile("abap-assistant", ".env");
        Files.writeString(temp, String.join(System.lineSeparator(),
            "# comment",
            "OPENAI_API_KEY=\"test-value\"",
            "OPENAI_MODEL=gpt-5-mini"));

        Map<String, String> values = new DotEnvLoader().load(temp);
        assertEquals("test-value", values.get("OPENAI_API_KEY"), "Quoted dotenv values must be parsed.");
        assertEquals("gpt-5-mini", values.get("OPENAI_MODEL"), "Plain dotenv values must be parsed.");
        Files.deleteIfExists(temp);
    }

    private static void extractorReadsResponsesOutputText() {
        String json = String.join(System.lineSeparator(),
            "{",
            "  \"output\": [{",
            "    \"type\": \"message\",",
            "    \"content\": [{",
            "      \"type\": \"output_text\",",
            "      \"text\": \"Line 1\\nLine 2\"",
            "    }]",
            "  }]",
            "}");

        String text = new OpenAiTextExtractor().extractOutputText(json).orElseThrow();
        assertEquals("Line 1\nLine 2", text, "Responses output_text must be extracted and unescaped.");
    }

    private static void assistantServiceUsesRedactedPrompt() throws Exception {
        AtomicReference<String> capturedPrompt = new AtomicReference<>();
        AiClient fakeClient = prompt -> {
            capturedPrompt.set(prompt);
            return "Mocked assistant answer";
        };

        AssistantService service = new AssistantService(
            new AssistantPromptBuilder(new SensitiveDataRedactor(), new AbapContextClassifier()),
            fakeClient);
        AssistantResponse response = service.answer(new AssistantRequest(
            AssistantMode.FIND_DEFECT,
            "Check ticket TCK99887",
            "DATA value TYPE string."));

        assertEquals("Mocked assistant answer", response.text(), "Service must return AI client answer.");
        assertTrue(capturedPrompt.get().contains("TCKXXXXX"), "Service must pass redacted prompts.");
        assertFalse(capturedPrompt.get().contains("TCK99887"), "Service must not pass raw ticket references.");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " Expected <" + expected + "> but got <" + actual + ">.");
        }
    }
}
