package io.github.mingzilla.llmclient;

/**
 * Represents a chunk of response from the streaming API
 * Corresponds to a single piece of a streamed response
 */
public record LlmClientOutputChunk(LlmClientMessage message, boolean done, int index) {
    /**
     * Parses a JSON string into an LlmClientOutputChunk
     * 
     * @param json The JSON string to parse
     * @return A new LlmClientOutputChunk
     */
    public static LlmClientOutputChunk fromJson(String json) {
        return LlmClientJsonUtil.fromJson(json, LlmClientOutputChunk.class);
    }

    /**
     * Creates an error chunk with the given message
     * 
     * @param message The error message
     * @return A new LlmClientOutputChunk representing an error
     */
    public static LlmClientOutputChunk forError(String message) {
        return new LlmClientOutputChunk(
            LlmClientMessage.assistant(message),
            true,
            -1);
    }
}