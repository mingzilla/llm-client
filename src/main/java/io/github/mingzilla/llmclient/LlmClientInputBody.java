package io.github.mingzilla.llmclient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Input body structure for chat completions API
 * Contains the parameters for a chat completions request
 */
public record LlmClientInputBody(String model, List<LlmClientMessage> messages,
        boolean stream, Double temperature) {

    /**
     * Creates a chat completion request body
     * 
     * @param model       Model identifier or null to use default
     * @param messages    Array of message objects with role and content
     * @param stream      Whether to stream the response
     * @param temperature Temperature value (0-1) or null for default
     * @return The created input body
     */
    public static LlmClientInputBody chat(String model, List<LlmClientMessage> messages,
            boolean stream, Double temperature) {
        return new LlmClientInputBody(model, messages, stream, temperature);
    }

    /**
     * Creates an SSE completion request body (always streaming)
     * 
     * @param model       Model identifier or null to use default
     * @param messages    Array of message objects with role and content
     * @param temperature Temperature value (0-1) or null for default
     * @return The created input body configured for SSE
     */
    public static LlmClientInputBody sse(String model, List<LlmClientMessage> messages,
            Double temperature) {
        return new LlmClientInputBody(model, messages, true, temperature);
    }

    /**
     * Creates a simple completion request with a single user message
     * 
     * @param content The user message content
     * @param stream  Whether to stream the response
     * @return The created input body
     */
    public static LlmClientInputBody chatMessage(String content, boolean stream) {
        return new LlmClientInputBody(
                null,
                List.of(LlmClientMessage.user(content)),
                stream,
                null);
    }

    /**
     * Converts the input body to a JSON-serializable map
     * 
     * @return A map of values ready for JSON serialization
     */
    public Map<String, Object> toJsonObject() {
        Map<String, Object> map = new HashMap<>();
        map.put("messages", messages);
        map.put("stream", stream);

        if (model != null)
            map.put("model", model);
        if (temperature != null)
            map.put("temperature", temperature);

        return map;
    }
}