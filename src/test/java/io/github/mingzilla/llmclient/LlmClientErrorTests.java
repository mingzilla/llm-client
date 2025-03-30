package io.github.mingzilla.llmclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LlmClientErrorTests {
    @Test
    void testFromException() {
        RuntimeException exception = new RuntimeException("Test error");
        LlmClientError error = LlmClientError.fromException(exception);

        assertEquals("Test error", error.message());
        assertEquals("RuntimeException", error.type());
        assertEquals("INTERNAL_ERROR", error.code());
    }

    @Test
    void testFromResponse() {
        LlmClientError error = LlmClientError.fromResponse(404, "Not Found", null);
        assertEquals("Not Found", error.message());
        assertEquals("ApiError", error.type());
        assertEquals("HTTP_404", error.code());
    }

    @Test
    void testFromResponseWithProviderCode() {
        LlmClientError error = LlmClientError.fromResponse(400, "Context length exceeded", "context_length_exceeded");
        assertEquals("Context length exceeded", error.message());
        assertEquals("ApiError", error.type());
        assertEquals("context_length_exceeded", error.code());
    }
}