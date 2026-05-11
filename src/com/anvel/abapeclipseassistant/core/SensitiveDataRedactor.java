package com.anvel.abapeclipseassistant.core;

import java.util.regex.Pattern;

public final class SensitiveDataRedactor {
    private static final Pattern OPENAI_KEY = Pattern.compile("sk-(?:proj-)?[A-Za-z0-9_-]{8,}");
    private static final Pattern TICKET = Pattern.compile("\\bTCK[A-Za-z0-9_-]{3,}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern HANDOVER = Pattern.compile("\\bHND[A-Za-z0-9_-]{3,}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVOICE = Pattern.compile("\\bINV[A-Za-z0-9_-]{3,}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL = Pattern.compile("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLIENT_NUMBER = Pattern.compile("\\b(client|mandant|mandante)\\s*[:=]?\\s*\\d{3}\\b", Pattern.CASE_INSENSITIVE);

    public String redact(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String result = OPENAI_KEY.matcher(value).replaceAll("[REDACTED_OPENAI_KEY]");
        result = TICKET.matcher(result).replaceAll("TCKXXXXX");
        result = HANDOVER.matcher(result).replaceAll("HNDXXXXX");
        result = INVOICE.matcher(result).replaceAll("INVXXXXX");
        result = EMAIL.matcher(result).replaceAll("user@example.invalid");
        result = CLIENT_NUMBER.matcher(result).replaceAll("$1 CLIENT_A");
        return result;
    }
}

