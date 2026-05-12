package com.anvel.abapeclipseassistant.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class OpenAiResponsesClient implements AiClient {
    private static final String INSTRUCTIONS = String.join(System.lineSeparator(),
        "You are a careful SAP ABAP and Eclipse ADT assistant.",
        "Provide practical guidance, keep public SAP knowledge separate from custom client code,",
        "and do not invent unsupported evidence.");

    private final OpenAiSettings settings;
    private final HttpClient httpClient;
    private final OpenAiTextExtractor textExtractor;
    private final SensitiveDataRedactor redactor;

    public OpenAiResponsesClient(OpenAiSettings settings) {
        this(settings, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build(), new OpenAiTextExtractor(), new SensitiveDataRedactor());
    }

    OpenAiResponsesClient(OpenAiSettings settings, HttpClient httpClient, OpenAiTextExtractor textExtractor, SensitiveDataRedactor redactor) {
        this.settings = settings;
        this.httpClient = httpClient;
        this.textExtractor = textExtractor;
        this.redactor = redactor;
    }

    @Override
    public String complete(String prompt) throws IOException, InterruptedException {
        String requestBody = String.format(
            "{%n" +
                "  \"model\": \"%s\",%n" +
                "  \"instructions\": \"%s\",%n" +
                "  \"input\": \"%s\",%n" +
                "  \"store\": false%n" +
                "}",
            JsonStrings.escape(settings.model()),
            JsonStrings.escape(INSTRUCTIONS.strip()),
            JsonStrings.escape(prompt));

        HttpRequest request = HttpRequest.newBuilder(URI.create(settings.endpoint()))
            .timeout(Duration.ofSeconds(90))
            .header("Authorization", "Bearer " + settings.apiKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String safeBody = redactor.redact(trim(response.body(), 1000));
            throw new IOException("OpenAI request failed with HTTP " + response.statusCode() + ": " + safeBody);
        }

        return textExtractor.extractOutputText(response.body())
            .orElseThrow(() -> new IOException("OpenAI response did not contain output_text."));
    }

    private static String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
