package io.github.mingzilla.llmclient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Input body structure for chat completions API
 * Contains the parameters for a chat completions request
 * 
 * @param model            Model identifier or null to use default
 * @param messages         Array of message objects with role and content
 * @param stream           Whether to stream the response
 * @param temperature      Temperature value (0-1) or null for default
 * @param additionalFields Provider-specific fields to include in the request.
 *                         These fields will be merged into the final request
 *                         body and can override the standard fields (model,
 *                         temperature, etc). Common uses include:
 *                         - Enforcing JSON responses (e.g., "response_format":
 *                         {"type": "json_object"})
 *                         - Setting provider-specific parameters
 *                         - Overriding default values
 */
public record LlmClientInputBody(String model, List<LlmClientMessage> messages,
        boolean stream, Double temperature,
        Map<String, Object> additionalFields) {

    /**
     * Creates a chat completion request body
     * 
     * @param model            Model identifier or null to use default
     * @param messages         Array of message objects with role and content
     * @param stream          Whether to stream the response
     * @param temperature     Temperature value (0-1) or null for default
     * @param additionalFields Provider-specific fields to include in the request
     * @return The created input body
     */
    public static LlmClientInputBody chat(String model, List<LlmClientMessage> messages,
            boolean stream, Double temperature,
            Map<String, Object> additionalFields) {
        return new LlmClientInputBody(model, messages, stream, temperature, additionalFields);
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
        return new LlmClientInputBody(model, messages, true, temperature, Map.of());
    }

    /**
     * Creates a simple completion request with a single user message
     * 
     * @param content          The user message content
     * @param stream           Whether to stream the response
     * @param additionalFields Provider-specific fields to include in the request
     * @return The created input body
     */
    public static LlmClientInputBody chatMessage(String content, boolean stream,
            Map<String, Object> additionalFields) {
        return new LlmClientInputBody(
                null,
                List.of(LlmClientMessage.user(content)),
                stream,
                null, additionalFields);
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
        if (additionalFields != null) {
            map.putAll(additionalFields);
        }

        return map;
    }
}