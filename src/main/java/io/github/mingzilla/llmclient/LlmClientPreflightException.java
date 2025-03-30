package io.github.mingzilla.llmclient;

/**
 * Exception thrown during the preflight phase of an LLM request.
 * This includes all processing that happens before the actual HTTP request is made,
 * such as input validation, resource checks, rate limiting, etc.
 */
public class LlmClientPreflightException extends RuntimeException {
    private final LlmClientOutput output;

    /**
     * Creates a new LlmClientPreflightException with the specified output
     * 
     * @param output The LlmClientOutput containing the failure details
     */
    public LlmClientPreflightException(LlmClientOutput output) {
        super(output.getFailureReason());
        this.output = output;
    }

    /**
     * Gets the LlmClientOutput associated with this exception
     * 
     * @return The LlmClientOutput containing the failure details
     */
    public LlmClientOutput getOutput() {
        return output;
    }
}