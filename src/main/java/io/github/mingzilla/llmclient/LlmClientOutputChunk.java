package io.github.mingzilla.llmclient;

/**
 * Represents a chunk of response from the streaming API
 * Corresponds to a single piece of a streamed response
 */
public record LlmClientOutputChunk(LlmClientMessage message, boolean done, int index,
        LlmClientError error) {
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
     * @param error The error
     * @return A new LlmClientOutputChunk representing an error
     */
    public static LlmClientOutputChunk forError(LlmClientError error) {
        return new LlmClientOutputChunk(
                LlmClientMessage.assistant(error.message()),
                true,
                -1, error);
    }
}