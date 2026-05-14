package com.abap.assistant.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AbapReferenceExtractor {
    private static final int MAX_REFERENCES = 20;
    private static final Pattern INCLUDE = Pattern.compile("(?im)^\\s*INCLUDE\\s+([A-Z0-9_/]+)\\s*\\.");
    private static final Pattern PERFORM_IN_PROGRAM = Pattern.compile("(?i)\\bPERFORM\\s+[A-Z0-9_]+\\s+IN\\s+PROGRAM\\s+([A-Z0-9_/]+)");
    private static final Pattern SUBMIT = Pattern.compile("(?i)\\bSUBMIT\\s+([A-Z0-9_/]+)");
    private static final Pattern CALL_FUNCTION = Pattern.compile("(?i)\\bCALL\\s+FUNCTION\\s+'([A-Z0-9_/]+)'");
    private static final Pattern CALL_TRANSACTION = Pattern.compile("(?i)\\bCALL\\s+TRANSACTION\\s+'?([A-Z0-9_]+)'?");
    private static final Pattern STATIC_CLASS_CALL = Pattern.compile("(?i)\\b((?:ZCL_|YCL_|CL_)[A-Z0-9_]+)=>");
    private static final Pattern CLASS_USAGE = Pattern.compile("(?i)\\b(?:NEW|TYPE\\s+REF\\s+TO|CREATE\\s+OBJECT|CLASS)\\s+((?:ZCL_|YCL_|CL_)[A-Z0-9_]+)");

    public List<String> extract(String abapText) {
        if (abapText == null || abapText.isBlank()) {
            return Collections.emptyList();
        }

        Set<String> references = new LinkedHashSet<>();
        collect(references, INCLUDE, abapText, "INCLUDE ");
        collect(references, PERFORM_IN_PROGRAM, abapText, "PERFORM IN PROGRAM ");
        collect(references, SUBMIT, abapText, "SUBMIT ");
        collect(references, CALL_FUNCTION, abapText, "CALL FUNCTION ");
        collect(references, CALL_TRANSACTION, abapText, "CALL TRANSACTION ");
        collect(references, STATIC_CLASS_CALL, abapText, "CLASS ");
        collect(references, CLASS_USAGE, abapText, "CLASS ");
        return new ArrayList<>(references);
    }

    public List<String> extractNames(String abapText) {
        if (abapText == null || abapText.isBlank()) {
            return Collections.emptyList();
        }

        Set<String> references = new LinkedHashSet<>();
        collectNames(references, INCLUDE, abapText);
        collectNames(references, PERFORM_IN_PROGRAM, abapText);
        collectNames(references, SUBMIT, abapText);
        collectNames(references, CALL_FUNCTION, abapText);
        collectNames(references, CALL_TRANSACTION, abapText);
        collectNames(references, STATIC_CLASS_CALL, abapText);
        collectNames(references, CLASS_USAGE, abapText);
        return new ArrayList<>(references);
    }

    private static void collect(Set<String> references, Pattern pattern, String text, String prefix) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find() && references.size() < MAX_REFERENCES) {
            String value = matcher.group(1);
            if (value != null && !value.isBlank()) {
                references.add(prefix + value.toUpperCase());
            }
        }
    }

    private static void collectNames(Set<String> references, Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find() && references.size() < MAX_REFERENCES) {
            String value = matcher.group(1);
            if (value != null && !value.isBlank()) {
                references.add(value.toUpperCase());
            }
        }
    }
}
