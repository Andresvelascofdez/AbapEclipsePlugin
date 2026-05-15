package com.abap.assistant.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AbapAnalysisResult {
    private final List<AbapObjectReference> references;
    private final List<AbapRiskSignal> riskSignals;

    public AbapAnalysisResult(List<AbapObjectReference> references, List<AbapRiskSignal> riskSignals) {
        this.references = copyReferences(references);
        this.riskSignals = copySignals(riskSignals);
    }

    public List<AbapObjectReference> references() {
        return references;
    }

    public List<AbapRiskSignal> riskSignals() {
        return riskSignals;
    }

    public List<AbapObjectReference> customReferences() {
        List<AbapObjectReference> custom = new ArrayList<>();
        for (AbapObjectReference reference : references) {
            if (reference.isCustomObject()) {
                custom.add(reference);
            }
        }
        return Collections.unmodifiableList(custom);
    }

    public List<String> referenceNames() {
        Set<String> names = new LinkedHashSet<>();
        for (AbapObjectReference reference : references) {
            if (!reference.name().isBlank()) {
                names.add(reference.name());
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(names));
    }

    public String summaryText() {
        return summaryText(Collections.emptyList());
    }

    public String summaryText(List<String> unresolvedReferences) {
        StringBuilder builder = new StringBuilder();
        builder.append("ABAP dependency/risk summary").append(System.lineSeparator());
        builder.append("- References detected: ").append(references.size()).append(System.lineSeparator());
        builder.append("- Custom/Z objects detected: ").append(customReferences().size()).append(System.lineSeparator());
        builder.append("- Risk signals detected: ").append(riskSignals.size()).append(System.lineSeparator());
        if (unresolvedReferences != null && !unresolvedReferences.isEmpty()) {
            builder.append("- Unresolved references: ").append(unresolvedReferences.size()).append(System.lineSeparator());
        }

        appendReferenceSection(builder, "References", references);
        appendReferenceSection(builder, "Custom/Z objects", customReferences());
        appendRiskSection(builder, riskSignals);
        if (unresolvedReferences != null && !unresolvedReferences.isEmpty()) {
            builder.append("Unresolved references").append(System.lineSeparator());
            for (String unresolved : unresolvedReferences) {
                builder.append("- ").append(unresolved).append(System.lineSeparator());
            }
        }
        return builder.toString().strip();
    }

    private static List<AbapObjectReference> copyReferences(List<AbapObjectReference> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(new LinkedHashSet<>(values)));
    }

    private static List<AbapRiskSignal> copySignals(List<AbapRiskSignal> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(new LinkedHashSet<>(values)));
    }

    private static void appendReferenceSection(StringBuilder builder, String title, List<AbapObjectReference> values) {
        if (values.isEmpty()) {
            return;
        }
        builder.append(title).append(System.lineSeparator());
        for (AbapObjectReference reference : values) {
            builder.append("- ").append(reference.display()).append(System.lineSeparator());
        }
    }

    private static void appendRiskSection(StringBuilder builder, List<AbapRiskSignal> values) {
        if (values.isEmpty()) {
            return;
        }
        builder.append("Risk signals").append(System.lineSeparator());
        for (AbapRiskSignal signal : values) {
            builder.append("- ").append(signal.display()).append(System.lineSeparator());
        }
    }
}
