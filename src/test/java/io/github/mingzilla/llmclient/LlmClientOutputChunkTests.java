package io.github.mingzilla.llmclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LlmClientOutputChunkTests {
    @Test
    void testFromJson() {
        String json = "{\"message\":{\"role\":\"assistant\",\"content\":\"Hello\"},\"done\":false,\"index\":0}";

        LlmClientOutputChunk chunk = LlmClientOutputChunk.fromJson(json);

        assertNotNull(chunk);
        assertEquals("assistant", chunk.message().role());
        assertEquals("Hello", chunk.message().content());
        assertFalse(chunk.done());
        assertEquals(0, chunk.index());
    }

    @Test
    void testFromInvalidJson() {
        String invalidJson = "invalid json";

        assertThrows(RuntimeException.class, () -> {
            LlmClientOutputChunk.fromJson(invalidJson);
        });
    }
}