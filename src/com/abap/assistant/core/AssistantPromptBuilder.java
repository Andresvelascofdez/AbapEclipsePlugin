package com.abap.assistant.core;

public final class AssistantPromptBuilder {
    private final SensitiveDataRedactor redactor;
    private final AbapContextClassifier classifier;
    private final AbapDependencyAnalyzer dependencyAnalyzer;

    public AssistantPromptBuilder(SensitiveDataRedactor redactor, AbapContextClassifier classifier) {
        this.redactor = redactor;
        this.classifier = classifier;
        this.dependencyAnalyzer = new AbapDependencyAnalyzer();
    }

    public BuiltPrompt build(AssistantRequest request) {
        String original = request.combinedInput();
        String redactedQuestion = redactor.redact(request.userText());
        String redactedCode = redactor.redact(request.selectedCode());
        PrivacyScope privacyScope = classifier.classify(original);
        boolean redacted = !original.equals(redactor.redact(original));
        AbapAnalysisResult analysis = dependencyAnalyzer.analyze(redactedCode);
        String detectedReferences = formatReferences(analysis.references());

        String template = String.join(System.lineSeparator(),
            "You are ABAP Chat Assistant, helping with SAP ABAP and Eclipse ADT work.",
            "",
            "Operating rules:",
            "- Keep SAP standard/public knowledge separate from client-specific Z/Y/private knowledge.",
            "- Treat custom Z/Y objects and namespace objects as potentially client-specific.",
            "- Do not invent evidence, dates, logs, hours, commits, screenshots, tickets or usage.",
            "- Use anonymised placeholders such as CLIENT_A, CLIENT_B, TCKXXXXX and HNDXXXXX.",
            "- If a conclusion is uncertain, include a TODO/TBC note instead of making unsupported claims.",
            "- Prefer safe, incremental ABAP or ADT guidance that preserves existing behaviour.",
            "- The user can ask in free text. Answer naturally, but stay precise and technical.",
            "- The editor context may contain every open Eclipse editor tab, not only the visible one.",
            "- If the user asks for code, provide ABAP snippets or patch-style suggestions only; do not claim that code was applied in SAP.",
            "- If related includes, submitted programs, function modules or transactions are referenced but their source is not provided, list them as additional context needed.",
            "",
            "Requested mode: %s",
            "Privacy scope detected before redaction: %s",
            "",
            "User question:",
            "%s",
            "",
            "ABAP or Eclipse editor context:",
            "%s",
            "",
            "Local ABAP dependency and risk analysis:",
            "%s",
            "",
            "Detected related ABAP references in provided context:",
            "%s",
            "",
            "Return:",
            "- For quick free-chat answers, use concise natural language.",
            "- For analysis or code review, lead with the practical answer, then risks and suggested ABAP code if useful.",
            "- Put proposed code in fenced ABAP blocks.",
            "- Include TODO/TBC only when evidence is missing.");
        String prompt = String.format(template,
            request.mode().label(),
            privacyScope,
            blankFallback(redactedQuestion),
            blankFallback(redactedCode),
            blankFallback(analysis.summaryText()),
            blankFallback(detectedReferences));

        return new BuiltPrompt(prompt.strip(), privacyScope, redacted);
    }

    private static String formatReferences(java.util.List<AbapObjectReference> references) {
        if (references == null || references.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (AbapObjectReference reference : references) {
            builder.append("- ").append(reference.display()).append(System.lineSeparator());
        }
        return builder.toString().strip();
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
