package com.abap.assistant.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AbapDependencyAnalyzer {
    private static final Pattern INCLUDE = Pattern.compile("(?i)^\\s*INCLUDE\\s+([A-Z0-9_/]+)\\s*\\.");
    private static final Pattern PERFORM_IN_PROGRAM = Pattern.compile("(?i)\\bPERFORM\\s+([A-Z0-9_]+)\\s+IN\\s+PROGRAM\\s+([A-Z0-9_/]+)");
    private static final Pattern PERFORM = Pattern.compile("(?i)\\bPERFORM\\s+([A-Z0-9_]+)\\b(?!\\s+IN\\s+PROGRAM)");
    private static final Pattern SUBMIT = Pattern.compile("(?i)\\bSUBMIT\\s+([A-Z0-9_/]+)");
    private static final Pattern CALL_FUNCTION = Pattern.compile("(?i)\\bCALL\\s+FUNCTION\\s+'([A-Z0-9_/]+)'");
    private static final Pattern CALL_TRANSACTION = Pattern.compile("(?i)\\bCALL\\s+TRANSACTION\\s+'?([A-Z0-9_]+)'?");
    private static final Pattern STATIC_METHOD = Pattern.compile("(?i)\\b((?:[ZY]CL_|CL_)[A-Z0-9_]+|/[A-Z0-9_]+/CL_[A-Z0-9_]+)=>\\s*([A-Z0-9_]+)?");
    private static final Pattern CLASS_USAGE = Pattern.compile("(?i)\\b(?:NEW|TYPE\\s+REF\\s+TO|CREATE\\s+OBJECT|CLASS)\\s+((?:[ZY]CL_|CL_)[A-Z0-9_]+|/[A-Z0-9_]+/CL_[A-Z0-9_]+)");
    private static final Pattern TABLES = Pattern.compile("(?i)^\\s*TABLES\\s+([A-Z0-9_/]+)");
    private static final Pattern SELECT_TABLE = Pattern.compile("(?i)\\b(?:FROM|JOIN)\\s+([A-Z0-9_/]+)");
    private static final Pattern WRITE_TABLE = Pattern.compile("(?i)^\\s*(?:UPDATE|MODIFY|DELETE)\\s+(?:FROM\\s+)?([A-Z0-9_/]+)");

    private final AbapRiskAnalyzer riskAnalyzer;

    public AbapDependencyAnalyzer() {
        this(new AbapRiskAnalyzer());
    }

    public AbapDependencyAnalyzer(AbapRiskAnalyzer riskAnalyzer) {
        this.riskAnalyzer = riskAnalyzer == null ? new AbapRiskAnalyzer() : riskAnalyzer;
    }

    public AbapAnalysisResult analyze(String abapText) {
        if (abapText == null || abapText.isBlank()) {
            return new AbapAnalysisResult(List.of(), List.of());
        }

        Set<AbapObjectReference> references = new LinkedHashSet<>();
        String[] lines = abapText.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            int lineNumber = index + 1;
            String line = stripComment(lines[index]);
            if (line.isBlank()) {
                continue;
            }

            collect(references, INCLUDE, line, lineNumber, AbapObjectType.INCLUDE);
            collectPerformInProgram(references, line, lineNumber);
            collect(references, PERFORM, line, lineNumber, AbapObjectType.FORM);
            collect(references, SUBMIT, line, lineNumber, AbapObjectType.PROGRAM);
            collect(references, CALL_FUNCTION, line, lineNumber, AbapObjectType.FUNCTION_MODULE);
            collect(references, CALL_TRANSACTION, line, lineNumber, AbapObjectType.TRANSACTION);
            collectStaticMethod(references, line, lineNumber);
            collect(references, CLASS_USAGE, line, lineNumber, AbapObjectType.CLASS);
            collect(references, TABLES, line, lineNumber, AbapObjectType.TABLE);
            collect(references, SELECT_TABLE, line, lineNumber, AbapObjectType.TABLE);
            collect(references, WRITE_TABLE, line, lineNumber, AbapObjectType.TABLE);
        }

        return new AbapAnalysisResult(new ArrayList<>(references), riskAnalyzer.analyze(abapText));
    }

    private static void collect(
        Set<AbapObjectReference> references,
        Pattern pattern,
        String line,
        int lineNumber,
        AbapObjectType type) {

        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            add(references, type, matcher.group(1), lineNumber, line);
        }
    }

    private static void collectPerformInProgram(Set<AbapObjectReference> references, String line, int lineNumber) {
        Matcher matcher = PERFORM_IN_PROGRAM.matcher(line);
        while (matcher.find()) {
            add(references, AbapObjectType.FORM, matcher.group(1), lineNumber, line);
            add(references, AbapObjectType.PROGRAM, matcher.group(2), lineNumber, line);
        }
    }

    private static void collectStaticMethod(Set<AbapObjectReference> references, String line, int lineNumber) {
        Matcher matcher = STATIC_METHOD.matcher(line);
        while (matcher.find()) {
            String className = matcher.group(1);
            String methodName = matcher.group(2);
            add(references, AbapObjectType.CLASS, className, lineNumber, line);
            if (methodName != null && !methodName.isBlank()) {
                add(references, AbapObjectType.METHOD, className + "=>" + methodName, lineNumber, line);
            }
        }
    }

    private static void add(
        Set<AbapObjectReference> references,
        AbapObjectType type,
        String name,
        int lineNumber,
        String evidence) {

        String normalized = AbapObjectReference.normalizeName(name);
        if (!normalized.isBlank()) {
            references.add(new AbapObjectReference(type, normalized, lineNumber, evidence));
        }
    }

    private static String stripComment(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.stripLeading();
        if (trimmed.startsWith("*") || trimmed.startsWith("\"")) {
            return "";
        }
        int inlineComment = line.indexOf('"');
        return inlineComment >= 0 ? line.substring(0, inlineComment) : line;
    }
}
