package io.github.mingzilla.llmclient;

import java.util.Map;

import org.springframework.http.HttpHeaders;

/**
 * Input contract for HTTP requests to LLM API
 * Represents a complete request to the LLM API
 */
public record LlmClientInput(String url, String body,
        Map<String, String> headers, LlmClientInputBody inputBody) {

    /**
     * Creates an input for an LLM chat request
     * 
     * @param url       The complete URL to send the request to
     * @param inputBody The LlmClientInputBody containing the request parameters
     * @param headers   Headers for the request
     * @return A new LlmClientInput configured for chat completions
     */
    public static LlmClientInput chat(String url, LlmClientInputBody inputBody,
            Map<String, String> headers) {
        return new LlmClientInput(
                url,
                LlmClientJsonUtil.toJson(inputBody.toJsonObject()),
                headers,
                inputBody);
    }

    /**
     * Sets HTTP headers for the request
     * Adds all headers from this input's headers map to the provided HttpHeaders
     * object
     * 
     * @param headers The HttpHeaders object to update with this input's headers
     */
    public void setHeaders(HttpHeaders headers) {
        if (this.headers() != null)
            this.headers().forEach(headers::add);
    }
}