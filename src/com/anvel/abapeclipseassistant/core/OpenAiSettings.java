package com.anvel.abapeclipseassistant.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public record OpenAiSettings(String apiKey, String model, String endpoint) {
    public static final String DEFAULT_MODEL = "gpt-5-mini";
    public static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/responses";

    public OpenAiSettings {
        apiKey = required(apiKey, "OPENAI_API_KEY");
        model = optional(model).orElse(DEFAULT_MODEL);
        endpoint = optional(endpoint).orElse(DEFAULT_ENDPOINT);
    }

    public static OpenAiSettings fromEnvironment() throws IOException {
        Map<String, String> dotenv = loadDotEnv();
        String apiKey = firstPresent(
            System.getProperty("OPENAI_API_KEY"),
            System.getenv("OPENAI_API_KEY"),
            dotenv.get("OPENAI_API_KEY"));
        String model = firstPresent(
            System.getProperty("OPENAI_MODEL"),
            System.getenv("OPENAI_MODEL"),
            dotenv.get("OPENAI_MODEL"),
            DEFAULT_MODEL);
        String endpoint = firstPresent(
            System.getProperty("OPENAI_BASE_URL"),
            System.getenv("OPENAI_BASE_URL"),
            dotenv.get("OPENAI_BASE_URL"),
            DEFAULT_ENDPOINT);

        return new OpenAiSettings(apiKey, model, endpoint);
    }

    private static Map<String, String> loadDotEnv() throws IOException {
        String explicitPath = firstPresent(System.getProperty("ABAP_ECLIPSE_ASSISTANT_ENV_FILE"), System.getenv("ABAP_ECLIPSE_ASSISTANT_ENV_FILE"));
        Path path = explicitPath == null ? Path.of(".env") : Path.of(explicitPath);
        return new DotEnvLoader().load(path);
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
}

