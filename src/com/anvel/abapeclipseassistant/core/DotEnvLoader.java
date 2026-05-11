package com.anvel.abapeclipseassistant.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DotEnvLoader {
    public Map<String, String> load(Path path) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        if (path == null || !Files.isRegularFile(path)) {
            return values;
        }

        for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int equalsIndex = line.indexOf('=');
            if (equalsIndex <= 0) {
                continue;
            }

            String key = line.substring(0, equalsIndex).strip();
            String value = line.substring(equalsIndex + 1).strip();
            values.put(key, unquote(value));
        }

        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}

