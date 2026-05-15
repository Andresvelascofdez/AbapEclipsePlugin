package com.abap.assistant.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class AssistantCoreTest {
    public static void main(String[] args) throws Exception {
        redactorRemovesSensitiveValues();
        classifierSeparatesCustomAndStandardContext();
        promptBuilderAppliesProjectRules();
        promptBuilderSupportsFreeChatAndRelatedReferences();
        referenceExtractorFindsNestedAbapHints();
        dependencyAnalyzerFindsReferencesAndCustomObjects();
        riskAnalyzerFindsRiskSignals();
        promptBuilderIncludesLocalAbapAnalysis();
        suggestedChangeReviewAddsSafeHeaderAndNoWriteText();
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
        assertEquals(PrivacyScope.SAP_STANDARD_PUBLIC, classifier.classify("Use BAPI_TRANSACTION_COMMIT with ADT."), "SAP hints are public standard context.");
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

    private static void promptBuilderSupportsFreeChatAndRelatedReferences() {
        AssistantPromptBuilder builder = new AssistantPromptBuilder(new SensitiveDataRedactor(), new AbapContextClassifier());
        AssistantPromptBuilder.BuiltPrompt prompt = builder.build(new AssistantRequest(
            AssistantMode.FREE_CHAT,
            "Rewrite this safely but do not apply it.",
            "INCLUDE zrate_forms.\nCALL FUNCTION 'Z_RATE_SAVE'."));

        assertTrue(prompt.prompt().contains("provide ABAP snippets or patch-style suggestions only"), "Prompt must support code suggestions without claiming changes were applied.");
        assertTrue(prompt.prompt().contains("Include ZRATE_FORMS"), "Prompt must include detected include references.");
        assertTrue(prompt.prompt().contains("Function module Z_RATE_SAVE"), "Prompt must include detected function module references.");
    }

    private static void referenceExtractorFindsNestedAbapHints() {
        AbapReferenceExtractor extractor = new AbapReferenceExtractor();
        java.util.List<String> references = extractor.extract(String.join(System.lineSeparator(),
            "INCLUDE zrate_top.",
            "PERFORM run IN PROGRAM zrate_runner.",
            "SUBMIT zrate_report.",
            "CALL TRANSACTION 'SE38'.",
            "DATA(lo_helper) = NEW zcl_rate_helper( ).",
            "zcl_rate_helper=>run( )."));

        assertTrue(references.contains("INCLUDE ZRATE_TOP"), "Include references must be detected.");
        assertTrue(references.contains("PERFORM IN PROGRAM ZRATE_RUNNER"), "PERFORM IN PROGRAM references must be detected.");
        assertTrue(references.contains("SUBMIT ZRATE_REPORT"), "SUBMIT references must be detected.");
        assertTrue(references.contains("CALL TRANSACTION SE38"), "CALL TRANSACTION references must be detected.");
        assertTrue(references.contains("CLASS ZCL_RATE_HELPER"), "Class references must be detected.");
        assertTrue(extractor.extractNames("INCLUDE zrate_top.\nzcl_rate_helper=>run( ).").contains("ZCL_RATE_HELPER"), "Raw reference names must be available for workspace lookup.");
    }

    private static void dependencyAnalyzerFindsReferencesAndCustomObjects() {
        AbapDependencyAnalyzer analyzer = new AbapDependencyAnalyzer();
        AbapAnalysisResult result = analyzer.analyze(String.join(System.lineSeparator(),
            "TABLES mara.",
            "INCLUDE zrate_top.",
            "PERFORM build_data.",
            "PERFORM run IN PROGRAM zrate_runner.",
            "SUBMIT zrate_report.",
            "CALL FUNCTION 'Z_RATE_SAVE'.",
            "CALL TRANSACTION 'SE38'.",
            "DATA(lo_helper) = NEW zcl_rate_helper( ).",
            "zcl_rate_helper=>run( ).",
            "SELECT * FROM zrate_cfg INTO TABLE @DATA(lt_cfg)."));

        assertTrue(hasReference(result, AbapObjectType.TABLE, "MARA"), "TABLES references must be detected.");
        assertTrue(hasReference(result, AbapObjectType.INCLUDE, "ZRATE_TOP"), "Include references must be detected.");
        assertTrue(hasReference(result, AbapObjectType.FORM, "BUILD_DATA"), "Local PERFORM references must be detected.");
        assertTrue(hasReference(result, AbapObjectType.PROGRAM, "ZRATE_RUNNER"), "PERFORM IN PROGRAM target must be detected.");
        assertTrue(hasReference(result, AbapObjectType.PROGRAM, "ZRATE_REPORT"), "SUBMIT target must be detected.");
        assertTrue(hasReference(result, AbapObjectType.FUNCTION_MODULE, "Z_RATE_SAVE"), "Function module references must be detected.");
        assertTrue(hasReference(result, AbapObjectType.TRANSACTION, "SE38"), "Transaction references must be detected.");
        assertTrue(hasReference(result, AbapObjectType.CLASS, "ZCL_RATE_HELPER"), "Class references must be detected.");
        assertTrue(hasReference(result, AbapObjectType.METHOD, "ZCL_RATE_HELPER=>RUN"), "Static method calls must be detected.");
        assertTrue(hasReference(result, AbapObjectType.TABLE, "ZRATE_CFG"), "SELECT FROM table references must be detected.");
        assertTrue(result.customReferences().size() >= 6, "Z/custom references must be separated for summary and UI display.");
    }

    private static void riskAnalyzerFindsRiskSignals() {
        AbapRiskAnalyzer analyzer = new AbapRiskAnalyzer();
        java.util.List<AbapRiskSignal> signals = analyzer.analyze(String.join(System.lineSeparator(),
            "LOOP AT lt_rates INTO DATA(ls_rate).",
            "  SELECT * FROM zrate_cfg INTO TABLE @DATA(lt_cfg).",
            "ENDLOOP.",
            "COMMIT WORK.",
            "ROLLBACK WORK.",
            "CALL TRANSACTION 'SE38' USING gt_bdc.",
            "UPDATE zrate_cfg SET active = abap_true.",
            "AUTHORITY-CHECK OBJECT 'Z_RATE'.",
            "CALL FUNCTION 'ENQUEUE_E_TABLE'.",
            "CALL FUNCTION 'Z_RATE_SAVE' IN UPDATE TASK.",
            "IF mandt = '100'. ENDIF.",
            "lv_password = 'secret123'."));

        assertTrue(hasSignal(signals, AbapRiskAnalyzer.SELECT_INSIDE_LOOP), "SELECT inside LOOP must be detected.");
        assertTrue(hasSignal(signals, AbapRiskAnalyzer.COMMIT_WORK), "COMMIT WORK must be detected.");
        assertTrue(hasSignal(signals, AbapRiskAnalyzer.ROLLBACK_WORK), "ROLLBACK WORK must be detected.");
        assertTrue(hasSignal(signals, AbapRiskAnalyzer.CALL_TRANSACTION), "CALL TRANSACTION must be detected.");
        assertTrue(hasSignal(signals, AbapRiskAnalyzer.BDC_USAGE), "BDC usage must be detected.");
        assertTrue(hasSignal(signals, AbapRiskAnalyzer.DATABASE_WRITE), "Database write statements must be detected.");
        assertTrue(hasSignal(signals, AbapRiskAnalyzer.CUSTOM_TABLE_ACCESS), "Custom table access must be detected.");
        assertTrue(hasSignal(signals, AbapRiskAnalyzer.AUTHORITY_CHECK), "AUTHORITY-CHECK must be detected.");
        assertTrue(hasSignal(signals, AbapRiskAnalyzer.LOCK_HANDLING), "ENQUEUE/DEQUEUE must be detected.");
        assertTrue(hasSignal(signals, AbapRiskAnalyzer.UPDATE_TASK), "Update task usage must be detected.");
        assertTrue(hasSignal(signals, AbapRiskAnalyzer.HARDCODED_CLIENT), "Hardcoded client values must be detected.");
        assertTrue(hasSignal(signals, AbapRiskAnalyzer.HARDCODED_SECRET), "Hardcoded secret-like values must be detected.");
    }

    private static void promptBuilderIncludesLocalAbapAnalysis() {
        AssistantPromptBuilder builder = new AssistantPromptBuilder(new SensitiveDataRedactor(), new AbapContextClassifier());
        AssistantPromptBuilder.BuiltPrompt prompt = builder.build(new AssistantRequest(
            AssistantMode.FIND_DEFECT,
            "Find risks",
            String.join(System.lineSeparator(),
                "LOOP AT lt_rates INTO DATA(ls_rate).",
                "  SELECT * FROM zrate_cfg INTO TABLE @DATA(lt_cfg).",
                "ENDLOOP.",
                "COMMIT WORK.")));

        assertTrue(prompt.prompt().contains("Local ABAP dependency and risk analysis"), "Prompt must include local ABAP analysis.");
        assertTrue(prompt.prompt().contains("SELECT inside LOOP"), "Prompt must include risk signal summary.");
        assertTrue(prompt.prompt().contains("Custom/Z objects"), "Prompt must include custom object summary.");
    }

    private static void suggestedChangeReviewAddsSafeHeaderAndNoWriteText() {
        SuggestedChangeReviewBuilder builder = new SuggestedChangeReviewBuilder(new SafeChangeRules(), LocalDate.of(2026, 5, 15));
        SuggestedChangeReview review = builder.build(
            "WRITE: / 'old'.",
            String.join(System.lineSeparator(),
                "Replace the message text after review.",
                "```abap",
                "WRITE: / 'new'.",
                "```"));

        assertTrue(review.hasSuggestion(), "Fenced ABAP code must produce a reviewable suggestion.");
        assertTrue(review.copyText().contains("manual review required"), "Suggestion copy text must include manual review header.");
        assertTrue(review.copyText().contains("Original code should be retained/commented below"), "Suggestion copy text must preserve safe-change rule.");
        assertTrue(review.copyText().contains("Date: 2026-05-15"), "Suggestion header must include trace date.");
        assertTrue(review.displayText().contains("does not write to SAP"), "Review text must state the no-write rule.");
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

    private static boolean hasReference(AbapAnalysisResult result, AbapObjectType type, String name) {
        for (AbapObjectReference reference : result.references()) {
            if (reference.type() == type && reference.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSignal(java.util.List<AbapRiskSignal> signals, String code) {
        for (AbapRiskSignal signal : signals) {
            if (signal.code().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
