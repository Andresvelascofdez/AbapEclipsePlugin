package com.anvel.abapeclipseassistant.core;

@FunctionalInterface
public interface AiClient {
    String complete(String prompt) throws Exception;
}

