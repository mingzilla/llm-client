package io.github.mingzilla.llmclient;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON utilities for LLM client
 * Provides JSON parsing and serialization capabilities
 */
public class LlmClientJsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parses a JSON string into a simple class type
     * 
     * @param <T>   The type to parse the JSON into
     * @param json  The JSON string to parse
     * @param clazz The class to parse into
     * @return The parsed object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    /**
     * Parses a JSON string into a specific structure defined by a TypeReference
     * e.g. new TypeReference<Map<String, List>>() {}) returns a Map<String, List>
     * 
     * @param <T>           The type to parse the JSON into
     * @param json          The JSON string to parse
     * @param typeReference The TypeReference describing the type
     * @return The parsed object
     */
    public static <T> T fromJsonToStructure(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    /**
     * Converts an object to a JSON string
     * 
     * @param obj The object to convert
     * @return The JSON string
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to JSON", e);
        }
    }

    /**
     * Parses a streaming chunk from a JSON string
     * 
     * @param chunk The JSON string to parse
     * @return The parsed LlmClientOutputChunk
     */
    public static LlmClientOutputChunk parseStreamChunk(String chunk) {
        return fromJson(chunk, LlmClientOutputChunk.class);
    }

    /**
     * Determines if a chunk represents the end of a stream
     * 
     * @param chunk The chunk to check
     * @return true if the chunk is an end marker, false otherwise
     */
    public static boolean isStreamEnd(String chunk) {
        return chunk != null && chunk.contains("\"done\": true");
    }

    /**
     * Extracts error code from LLM provider error response
     * Handles different provider formats:
     * - OpenAI: {"error": {"code": "context_length_exceeded"}}
     * - Anthropic: {"error": {"type": "invalid_request_error"}}
     * - Generic: {"code": "error_code"}
     *
     * @param errorBody The error response body
     * @return The provider error code or null if not found
     */
    public static String extractErrorCode(String errorBody) {
        if (errorBody == null || errorBody.isEmpty()) return null;

        try {
            Map<String, Object> errorMap = fromJsonToStructure(errorBody, new TypeReference<Map<String, Object>>() {
            });
            if (errorMap == null) return null;

            Object code = errorMap.get("code"); // Try direct code field
            if (code instanceof String) return (String) code;

            Object error = errorMap.get("error"); // Try nested error object (OpenAI style)
            if (error instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> errorObj = (Map<String, Object>) error;
                code = errorObj.get("code");
                if (code instanceof String) return (String) code;
                code = errorObj.get("type"); // Some providers use 'type' instead of 'code'
                if (code instanceof String) return (String) code;
            }
        } catch (Exception ignore) {
            // Ignore parsing errors
        }
        return null;
    }
}