package io.github.mingzilla.llmclient;

import java.util.Map;

import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * Complete response from the LLM API
 * Represents either a successful response or an error
 */
public record LlmClientOutput(int statusCode, Map<String, String> headers,
        String body, LlmClientError error, LlmClientMessage message) {

    /**
     * Gets a specific header value
     * 
     * @param name The header name
     * @return The header value or null if not found
     */
    public String getHeader(String name) {
        return headers != null ? headers.get(name) : null;
    }

    /**
     * Parses the response body as JSON
     * 
     * @param <T>  The type to parse the JSON into
     * @param type The class to parse the JSON into
     * @return The parsed object
     */
    public <T> T parseJsonBody(Class<T> type) {
        return LlmClientJsonUtil.fromJson(body, type);
    }

    /**
     * Parses the response body as a JSON object
     *
     * @return The parsed JSON object
     */
    public Map<String, Object> asMap() {
        return Map.of(
                "statusCode", statusCode,
                "headers", headers,
                "body", body,
                "error", error,
                "message", message);
    }

    /**
     * Determines if the request was successful
     * 
     * @return true if successful, false otherwise
     */
    public boolean isSuccessful() {
        return error == null;
    }

    /**
     * Gets the failure reason if the request failed
     * 
     * @return The error message or null if successful
     */
    public String getFailureReason() {
        return error != null ? error.message() : null;
    }

    /**
     * Creates an output object for a successful response
     * 
     * @param response The WebFlux ClientResponse
     * @param body     The response body
     * @return A new LlmClientOutput with success data
     */
    public static LlmClientOutput forSuccess(ClientResponse response, String body) {
        return new LlmClientOutput(
                response.statusCode().value(),
                response.headers().asHttpHeaders().toSingleValueMap(),
                body,
                null,
                null);
    }

    /**
     * Creates an output object representing successful verification
     * 
     * @return A new LlmClientOutput indicating verification success
     */
    public static LlmClientOutput forSuccessVerification() {
        return new LlmClientOutput(200, Map.of(), null, null, null);
    }

    /**
     * Creates a response from a WebClient response
     * 
     * @param response The WebClient response
     * @param body     The response body as string
     * @return A new LlmClientOutput instance
     */
    public static LlmClientOutput fromResponse(ClientResponse response, String body) {
        if (response.statusCode().is2xxSuccessful()) {
            return forSuccess(response, body);
        } else {
            return forError(LlmClientError.fromResponse(
                    response.statusCode().value(),
                    body,
                    LlmClientJsonUtil.extractErrorCode(body)));
        }
    }

    /**
     * Creates an output object for an error
     * 
     * @param error The LlmClientError, must not be null
     * @return A new LlmClientOutput with the error set
     * @throws IllegalArgumentException if error is null
     */
    public static LlmClientOutput forError(LlmClientError error) {
        if (error == null) {
            throw new IllegalArgumentException("Error must not be null");
        }
        return new LlmClientOutput(500, Map.of(), null, error, null);
    }

    /**
     * Creates an output object for a 401 Unauthorized error
     * 
     * @return A new LlmClientOutput with 401 error
     */
    public static LlmClientOutput forError401() {
        return forError(LlmClientError.create401());
    }
}