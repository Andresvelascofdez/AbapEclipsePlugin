package com.abap.assistant.core;

import java.util.Locale;
import java.util.Objects;

public final class AbapObjectReference {
    private final AbapObjectType type;
    private final String name;
    private final int lineNumber;
    private final String evidence;

    public AbapObjectReference(AbapObjectType type, String name, int lineNumber, String evidence) {
        this.type = type == null ? AbapObjectType.UNKNOWN : type;
        this.name = normalizeName(name);
        this.lineNumber = Math.max(0, lineNumber);
        this.evidence = evidence == null ? "" : evidence.strip();
    }

    public AbapObjectType type() {
        return type;
    }

    public String name() {
        return name;
    }

    public int lineNumber() {
        return lineNumber;
    }

    public String evidence() {
        return evidence;
    }

    public boolean isCustomObject() {
        return isCustomName(name);
    }

    public String display() {
        String location = lineNumber > 0 ? " at line " + lineNumber : "";
        return type.displayName() + " " + name + location;
    }

    static boolean isCustomName(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = normalizeName(value);
        return normalized.startsWith("Z")
            || normalized.startsWith("Y")
            || normalized.matches("/[A-Z0-9_]+/.*");
    }

    static String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.strip()
            .toUpperCase(Locale.ROOT)
            .replaceAll("[,.;:()]+$", "");
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AbapObjectReference)) {
            return false;
        }
        AbapObjectReference that = (AbapObjectReference) other;
        return type == that.type && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }
}
