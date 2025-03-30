package io.github.mingzilla.llmclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LlmClientMessageTests {
    @Test
    void testSystemMessage() {
        LlmClientMessage message = LlmClientMessage.system("Test instruction");
        assertEquals("system", message.role());
        assertEquals("Test instruction", message.content());
    }

    @Test
    void testUserMessage() {
        LlmClientMessage message = LlmClientMessage.user("User input");
        assertEquals("user", message.role());
        assertEquals("User input", message.content());
    }

    @Test
    void testAssistantMessage() {
        LlmClientMessage message = LlmClientMessage.assistant("Assistant response");
        assertEquals("assistant", message.role());
        assertEquals("Assistant response", message.content());
    }
}