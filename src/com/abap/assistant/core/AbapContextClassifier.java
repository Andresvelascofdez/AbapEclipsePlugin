package com.abap.assistant.core;

import java.util.regex.Pattern;

public final class AbapContextClassifier {
    private static final Pattern CUSTOM_OBJECT = Pattern.compile("\\b[ZY][A-Z0-9_]{2,}\\b|/[A-Z0-9_]+/[A-Z0-9_]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern STANDARD_HINT = Pattern.compile("\\b(SAP|ABAP|ADT|BAPI_[A-Z0-9_]*|CL_[A-Z0-9_]*|IF_[A-Z0-9_]*|DD02L|TADIR)\\b", Pattern.CASE_INSENSITIVE);

    public PrivacyScope classify(String text) {
        if (text == null || text.isBlank()) {
            return PrivacyScope.MIXED_OR_UNKNOWN;
        }

        boolean hasCustomObject = CUSTOM_OBJECT.matcher(text).find();
        boolean hasStandardHint = STANDARD_HINT.matcher(text).find();

        if (hasCustomObject && hasStandardHint) {
            return PrivacyScope.MIXED_OR_UNKNOWN;
        }
        if (hasCustomObject) {
            return PrivacyScope.CLIENT_SPECIFIC_CUSTOM;
        }
        if (hasStandardHint) {
            return PrivacyScope.SAP_STANDARD_PUBLIC;
        }
        return PrivacyScope.MIXED_OR_UNKNOWN;
    }
}
