package io.github.mingzilla.llmclient;

/**
 * Verification helper for LLM client operations
 */
public final class LlmClientVerifier {
    private LlmClientVerifier() {
        // Prevent instantiation
    }

    /**
     * Verifies that a required component is not null
     * 
     * @param component The component to verify
     * @param name The name of the component for the error message
     * @throws IllegalArgumentException if the component is null
     */
    public static void require(Object component, String name) {
        if (component == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }
}