package com.abap.assistant.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class OpenAiTextExtractor {
    public Optional<String> extractOutputText(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        List<String> outputTexts = new ArrayList<>();
        int cursor = 0;
        while (cursor < json.length()) {
            int outputTypeIndex = json.indexOf("\"output_text\"", cursor);
            if (outputTypeIndex < 0) {
                break;
            }

            int textKeyIndex = json.indexOf("\"text\"", outputTypeIndex);
            if (textKeyIndex < 0) {
                break;
            }

            int colonIndex = json.indexOf(':', textKeyIndex);
            int quoteIndex = findNextQuote(json, colonIndex + 1);
            if (quoteIndex < 0) {
                break;
            }

            ParsedString parsed = parseJsonString(json, quoteIndex);
            outputTexts.add(parsed.value());
            cursor = parsed.nextIndex();
        }

        if (outputTexts.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(System.lineSeparator(), outputTexts).strip());
    }

    private static int findNextQuote(String text, int start) {
        for (int index = Math.max(0, start); index < text.length(); index++) {
            if (text.charAt(index) == '"') {
                return index;
            }
        }
        return -1;
    }

    private static ParsedString parseJsonString(String json, int openingQuoteIndex) {
        StringBuilder result = new StringBuilder();
        int index = openingQuoteIndex + 1;
        while (index < json.length()) {
            char ch = json.charAt(index++);
            if (ch == '"') {
                return new ParsedString(result.toString(), index);
            }
            if (ch != '\\' || index >= json.length()) {
                result.append(ch);
                continue;
            }

            char escaped = json.charAt(index++);
            switch (escaped) {
                case '"':
                    result.append('"');
                    break;
                case '\\':
                    result.append('\\');
                    break;
                case '/':
                    result.append('/');
                    break;
                case 'b':
                    result.append('\b');
                    break;
                case 'f':
                    result.append('\f');
                    break;
                case 'n':
                    result.append('\n');
                    break;
                case 'r':
                    result.append('\r');
                    break;
                case 't':
                    result.append('\t');
                    break;
                case 'u':
                    if (index + 4 <= json.length()) {
                        String hex = json.substring(index, index + 4);
                        result.append((char) Integer.parseInt(hex, 16));
                        index += 4;
                    }
                    break;
                default:
                    result.append(escaped);
                    break;
            }
        }
        return new ParsedString(result.toString(), index);
    }

    private static final class ParsedString {
        private final String value;
        private final int nextIndex;

        private ParsedString(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }

        private String value() {
            return value;
        }

        private int nextIndex() {
            return nextIndex;
        }
    }
}
