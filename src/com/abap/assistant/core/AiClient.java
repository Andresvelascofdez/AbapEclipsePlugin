package com.abap.assistant.core;

@FunctionalInterface
public interface AiClient {
    String complete(String prompt) throws Exception;
}

