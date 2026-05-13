package com.abap.assistant.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OpenAiSettings {
    public static final String DEFAULT_MODEL = "gpt-5-mini";
    public static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/responses";

    private final String apiKey;
    private final String model;
    private final String endpoint;

    public OpenAiSettings(String apiKey, String model, String endpoint) {
        this.apiKey = required(apiKey, "OPENAI_API_KEY");
        this.model = optional(model).orElse(DEFAULT_MODEL);
        this.endpoint = optional(endpoint).orElse(DEFAULT_ENDPOINT);
    }

    public String apiKey() {
        return apiKey;
    }

    public String model() {
        return model;
    }

    public String endpoint() {
        return endpoint;
    }

    public static OpenAiSettings fromEnvironment(Path... dotenvCandidates) throws IOException {
        DotEnvSearchResult dotenv = loadDotEnv(dotenvCandidates);
        String apiKey = firstPresent(
            System.getProperty("OPENAI_API_KEY"),
            System.getenv("OPENAI_API_KEY"),
            dotenv.values().get("OPENAI_API_KEY"));
        String model = firstPresent(
            System.getProperty("OPENAI_MODEL"),
            System.getenv("OPENAI_MODEL"),
            dotenv.values().get("OPENAI_MODEL"),
            DEFAULT_MODEL);
        String endpoint = firstPresent(
            System.getProperty("OPENAI_BASE_URL"),
            System.getenv("OPENAI_BASE_URL"),
            dotenv.values().get("OPENAI_BASE_URL"),
            DEFAULT_ENDPOINT);

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("OPENAI_API_KEY is required. Checked .env locations: " + dotenv.searchedPathsSummary());
        }
        return new OpenAiSettings(apiKey, model, endpoint);
    }

    private static DotEnvSearchResult loadDotEnv(Path... dotenvCandidates) throws IOException {
        List<Path> searchedPaths = new ArrayList<>();
        DotEnvLoader loader = new DotEnvLoader();
        String explicitPath = firstPresent(System.getProperty("ABAP_ECLIPSE_ASSISTANT_ENV_FILE"), System.getenv("ABAP_ECLIPSE_ASSISTANT_ENV_FILE"));
        if (explicitPath != null) {
            Path path = Path.of(explicitPath);
            searchedPaths.add(path);
            return new DotEnvSearchResult(loader.load(path), searchedPaths);
        }

        for (Path candidate : dotenvCandidates) {
            if (candidate == null) {
                continue;
            }
            searchedPaths.add(candidate);
            Map<String, String> values = loader.load(candidate);
            if (!values.isEmpty()) {
                return new DotEnvSearchResult(values, searchedPaths);
            }
        }

        Path defaultPath = Path.of(".env");
        searchedPaths.add(defaultPath);
        return new DotEnvSearchResult(loader.load(defaultPath), searchedPaths);
    }

    private static String required(String value, String name) {
        return optional(value).orElseThrow(() -> new IllegalArgumentException(name + " is required."));
    }

    private static Optional<String> optional(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.strip());
    }

    private static String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.strip();
            }
        }
        return null;
    }

    private static final class DotEnvSearchResult {
        private final Map<String, String> values;
        private final List<Path> searchedPaths;

        private DotEnvSearchResult(Map<String, String> values, List<Path> searchedPaths) {
            this.values = values == null ? Collections.emptyMap() : values;
            this.searchedPaths = searchedPaths == null ? Collections.emptyList() : searchedPaths;
        }

        private Map<String, String> values() {
            return values;
        }

        private String searchedPathsSummary() {
            if (searchedPaths.isEmpty()) {
                return "(none)";
            }

            StringBuilder summary = new StringBuilder();
            for (int index = 0; index < searchedPaths.size(); index++) {
                if (index > 0) {
                    summary.append("; ");
                }
                summary.append(searchedPaths.get(index).toAbsolutePath());
            }
            return summary.toString();
        }
    }
}
