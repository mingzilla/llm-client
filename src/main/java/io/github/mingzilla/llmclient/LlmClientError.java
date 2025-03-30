package io.github.mingzilla.llmclient;

/**
 * Error representation for LLM API errors
 * Follows the error structure defined in the API specification
 */
public record LlmClientError(String message, String type,
        /**
         * Provider-specific error code from LLM response.
         * Examples:
         * - OpenAI: "context_length_exceeded", "rate_limit_exceeded"
         * - Anthropic: "overloaded_error", "invalid_request_error"
         * For internal errors: "INTERNAL_ERROR"
         * For HTTP transport errors without LLM error code: "HTTP_" + statusCode
         */
        String code) {
    /**
     * Creates an error from an exception
     * 
     * @param throwable The exception to convert
     * @return A new LlmClientError with appropriate message, type and code
     */
    public static LlmClientError fromException(Throwable throwable) {
        return new LlmClientError(
                throwable.getMessage(),
                throwable.getClass().getSimpleName(),
                "INTERNAL_ERROR");
    }

    /**
     * Creates an error from a response status and message
     * 
     * @param statusCode The HTTP status code
     * @param message    The error message
     * @return A new LlmClientError with appropriate type and code
     */
    /**
     * Creates an error from a response status, message, and provider-specific error
     * code
     * 
     * @param statusCode   The HTTP status code
     * @param message      The error message
     * @param providerCode Provider-specific error code from response (can be null)
     * @return A new LlmClientError with appropriate type and code
     */
    public static LlmClientError fromResponse(int statusCode, String message, String providerCode) {
        return new LlmClientError(
                message,
                "ApiError",
                providerCode != null ? providerCode : "HTTP_" + statusCode);
    }

    /**
     * Creates a standard 401 Unauthorized error
     * 
     * @return A new LlmClientError for unauthorized access
     */
    public static LlmClientError create401() {
        return new LlmClientError(
                "Unauthorized access",
                "AuthenticationError",
                "HTTP_401");
    }
}