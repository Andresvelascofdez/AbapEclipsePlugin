package com.anvel.abapeclipseassistant.core;

public final class AssistantPromptBuilder {
    private final SensitiveDataRedactor redactor;
    private final AbapContextClassifier classifier;

    public AssistantPromptBuilder(SensitiveDataRedactor redactor, AbapContextClassifier classifier) {
        this.redactor = redactor;
        this.classifier = classifier;
    }

    public BuiltPrompt build(AssistantRequest request) {
        String original = request.combinedInput();
        String redactedQuestion = redactor.redact(request.userText());
        String redactedCode = redactor.redact(request.selectedCode());
        PrivacyScope privacyScope = classifier.classify(original);
        boolean redacted = !original.equals(redactor.redact(original));

        String template = String.join(System.lineSeparator(),
            "You are ABAP Eclipse Assistant, helping with SAP ABAP and Eclipse ADT work.",
            "",
            "Operating rules:",
            "- Keep SAP standard/public knowledge separate from client-specific Z/Y/private knowledge.",
            "- Treat custom Z/Y objects and namespace objects as potentially client-specific.",
            "- Do not invent evidence, dates, logs, hours, commits, screenshots, tickets or usage.",
            "- Use anonymised placeholders such as CLIENT_A, CLIENT_B, TCKXXXXX and HNDXXXXX.",
            "- If a conclusion is uncertain, include a TODO/TBC note instead of making unsupported claims.",
            "- Prefer safe, incremental ABAP or ADT guidance that preserves existing behaviour.",
            "",
            "Requested mode: %s",
            "Privacy scope detected before redaction: %s",
            "",
            "User question:",
            "%s",
            "",
            "Selected ABAP or Eclipse context:",
            "%s",
            "",
            "Return:",
            "1. Short answer.",
            "2. Technical analysis.",
            "3. Safe next steps or checks.",
            "4. TODO/TBC items when evidence is missing.");
        String prompt = String.format(template,
            request.mode().label(),
            privacyScope,
            blankFallback(redactedQuestion),
            blankFallback(redactedCode));

        return new BuiltPrompt(prompt.strip(), privacyScope, redacted);
    }

    private static String blankFallback(String value) {
        return value == null || value.isBlank() ? "(none provided)" : value;
    }

    public static final class BuiltPrompt {
        private final String prompt;
        private final PrivacyScope privacyScope;
        private final boolean inputWasRedacted;

        public BuiltPrompt(String prompt, PrivacyScope privacyScope, boolean inputWasRedacted) {
            this.prompt = prompt;
            this.privacyScope = privacyScope;
            this.inputWasRedacted = inputWasRedacted;
        }

        public String prompt() {
            return prompt;
        }

        public PrivacyScope privacyScope() {
            return privacyScope;
        }

        public boolean inputWasRedacted() {
            return inputWasRedacted;
        }
    }
}
