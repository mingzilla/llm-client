package io.github.mingzilla.llmclient;

/**
 * Message record for chat completions API
 * Represents a single message in a conversation with the LLM
 */
public record LlmClientMessage(String role, String content) {
    /**
     * Creates a system message
     * 
     * @param content The system instruction
     * @return A new LlmClientMessage with role "system"
     */
    public static LlmClientMessage system(String content) {
        return new LlmClientMessage("system", content);
    }

    /**
     * Creates a user message
     * 
     * @param content The user's input
     * @return A new LlmClientMessage with role "user"
     */
    public static LlmClientMessage user(String content) {
        return new LlmClientMessage("user", content);
    }

    /**
     * Creates an assistant message
     * 
     * @param content The assistant's response
     * @return A new LlmClientMessage with role "assistant"
     */
    public static LlmClientMessage assistant(String content) {
        return new LlmClientMessage("assistant", content);
    }
}