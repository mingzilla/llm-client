package io.github.mingzilla.llmclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LlmClientInputBodyTests {
    @Test
    void testChat() {
        LlmClientInputBody body = LlmClientInputBody.chat(
                "gpt-3.5-turbo",
                List.of(LlmClientMessage.user("Hello")),
                false,
                0.7);

        assertEquals("gpt-3.5-turbo", body.model());
        assertFalse(body.stream());
        assertEquals(0.7, body.temperature());
        assertFalse(body.isSse());
    }

    @Test
    void testSse() {
        LlmClientInputBody body = LlmClientInputBody.sse(
                "gpt-3.5-turbo",
                List.of(LlmClientMessage.user("Hello")),
                0.7);

        assertTrue(body.stream());
        assertTrue(body.isSse());
    }

    @Test
    void testToJsonObject() {
        LlmClientInputBody body = LlmClientInputBody.chatMessage("Hello", false);
        Map<String, Object> json = body.toJsonObject();

        assertNotNull(json.get("messages"));
        assertEquals(false, json.get("stream"));
    }
}