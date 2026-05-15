package com.abap.assistant.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AbapRiskAnalyzer {
    public static final String SELECT_INSIDE_LOOP = "SELECT_INSIDE_LOOP";
    public static final String COMMIT_WORK = "COMMIT_WORK";
    public static final String ROLLBACK_WORK = "ROLLBACK_WORK";
    public static final String CALL_TRANSACTION = "CALL_TRANSACTION";
    public static final String BDC_USAGE = "BDC_USAGE";
    public static final String DATABASE_WRITE = "DATABASE_WRITE";
    public static final String CUSTOM_TABLE_ACCESS = "CUSTOM_TABLE_ACCESS";
    public static final String AUTHORITY_CHECK = "AUTHORITY_CHECK";
    public static final String LOCK_HANDLING = "LOCK_HANDLING";
    public static final String UPDATE_TASK = "UPDATE_TASK";
    public static final String HARDCODED_CLIENT = "HARDCODED_CLIENT";
    public static final String HARDCODED_SECRET = "HARDCODED_SECRET";

    private static final Pattern LOOP_START = Pattern.compile("(?i)\\bLOOP\\b");
    private static final Pattern LOOP_END = Pattern.compile("(?i)\\bENDLOOP\\b");
    private static final Pattern SELECT = Pattern.compile("(?i)\\bSELECT\\b");
    private static final Pattern COMMIT = Pattern.compile("(?i)\\bCOMMIT\\s+WORK\\b");
    private static final Pattern ROLLBACK = Pattern.compile("(?i)\\bROLLBACK\\s+WORK\\b");
    private static final Pattern CALL_TRANSACTION_PATTERN = Pattern.compile("(?i)\\bCALL\\s+TRANSACTION\\b");
    private static final Pattern BDC = Pattern.compile("(?i)\\b(BDCDATA|BDC_|CALL\\s+TRANSACTION\\b.*\\bUSING\\b)");
    private static final Pattern DATABASE_WRITE_PATTERN = Pattern.compile("(?i)^\\s*(UPDATE|MODIFY|DELETE)\\s+(?:FROM\\s+)?([A-Z0-9_/]+)");
    private static final Pattern CUSTOM_TABLE_READ = Pattern.compile("(?i)\\b(?:FROM|JOIN)\\s+([ZY][A-Z0-9_]+|/[A-Z0-9_]+/[A-Z0-9_]+)");
    private static final Pattern AUTHORITY = Pattern.compile("(?i)\\bAUTHORITY-CHECK\\b");
    private static final Pattern LOCK = Pattern.compile("(?i)\\b(ENQUEUE_|DEQUEUE_)");
    private static final Pattern UPDATE_TASK_PATTERN = Pattern.compile("(?i)\\b(IN\\s+UPDATE\\s+TASK|SET\\s+UPDATE\\s+TASK\\s+LOCAL)\\b");
    private static final Pattern HARDCODED_CLIENT_PATTERN = Pattern.compile("(?i)\\b(MANDT|CLIENT)\\b\\s*=\\s*'?\\d{3}'?");
    private static final Pattern HARDCODED_SECRET_PATTERN = Pattern.compile("(?i)\\b[A-Z0-9_]*(PASSWORD|PASSWD|PWD|API[_-]?KEY|SECRET|TOKEN)\\b\\s*=\\s*'[^']{4,}'");

    public List<AbapRiskSignal> analyze(String abapText) {
        if (abapText == null || abapText.isBlank()) {
            return List.of();
        }

        Set<AbapRiskSignal> signals = new LinkedHashSet<>();
        String[] lines = abapText.split("\\R", -1);
        int loopDepth = 0;

        for (int index = 0; index < lines.length; index++) {
            int lineNumber = index + 1;
            String line = stripComment(lines[index]);
            if (line.isBlank()) {
                continue;
            }

            if (LOOP_END.matcher(line).find() && loopDepth > 0) {
                loopDepth--;
            }
            if (loopDepth > 0 && SELECT.matcher(line).find()) {
                signals.add(signal(SELECT_INSIDE_LOOP, "SELECT inside LOOP", lineNumber, line));
            }

            addIfFound(signals, COMMIT, line, lineNumber, COMMIT_WORK, "COMMIT WORK");
            addIfFound(signals, ROLLBACK, line, lineNumber, ROLLBACK_WORK, "ROLLBACK WORK");
            addIfFound(signals, CALL_TRANSACTION_PATTERN, line, lineNumber, CALL_TRANSACTION, "CALL TRANSACTION");
            addIfFound(signals, BDC, line, lineNumber, BDC_USAGE, "BDC usage");
            addIfFound(signals, DATABASE_WRITE_PATTERN, line, lineNumber, DATABASE_WRITE, "Database write statement");
            addIfFound(signals, CUSTOM_TABLE_READ, line, lineNumber, CUSTOM_TABLE_ACCESS, "Direct access to custom table");
            addIfFound(signals, AUTHORITY, line, lineNumber, AUTHORITY_CHECK, "AUTHORITY-CHECK");
            addIfFound(signals, LOCK, line, lineNumber, LOCK_HANDLING, "ENQUEUE/DEQUEUE lock handling");
            addIfFound(signals, UPDATE_TASK_PATTERN, line, lineNumber, UPDATE_TASK, "Update task usage");
            addIfFound(signals, HARDCODED_CLIENT_PATTERN, line, lineNumber, HARDCODED_CLIENT, "Hardcoded client value");
            addIfFound(signals, HARDCODED_SECRET_PATTERN, line, lineNumber, HARDCODED_SECRET, "Hardcoded credential or secret-like value");

            if (LOOP_START.matcher(line).find()) {
                loopDepth++;
            }
        }

        return new ArrayList<>(signals);
    }

    private static void addIfFound(
        Set<AbapRiskSignal> signals,
        Pattern pattern,
        String line,
        int lineNumber,
        String code,
        String description) {

        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            signals.add(signal(code, description, lineNumber, line));
        }
    }

    private static AbapRiskSignal signal(String code, String description, int lineNumber, String evidence) {
        return new AbapRiskSignal(code, description, lineNumber, evidence);
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
