package com.abap.assistant.core;

import java.util.Objects;

public final class AbapRiskSignal {
    private final String code;
    private final String description;
    private final int lineNumber;
    private final String evidence;

    public AbapRiskSignal(String code, String description, int lineNumber, String evidence) {
        this.code = code == null || code.isBlank() ? "ABAP_RISK" : code.strip();
        this.description = description == null ? "" : description.strip();
        this.lineNumber = Math.max(0, lineNumber);
        this.evidence = evidence == null ? "" : evidence.strip();
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }

    public int lineNumber() {
        return lineNumber;
    }

    public String evidence() {
        return evidence;
    }

    public String display() {
        String location = lineNumber > 0 ? " at line " + lineNumber : "";
        return description + location;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AbapRiskSignal)) {
            return false;
        }
        AbapRiskSignal that = (AbapRiskSignal) other;
        return code.equals(that.code) && lineNumber == that.lineNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, lineNumber);
    }
}
