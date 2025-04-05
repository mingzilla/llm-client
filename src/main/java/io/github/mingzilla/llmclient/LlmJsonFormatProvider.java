package io.github.mingzilla.llmclient;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for adding provider-specific JSON format enforcement parameters to
 * LLM requests
 */
public class LlmJsonFormatProvider {

    /**
     * Returns provider-specific parameters to enforce JSON responses based on model
     * name. Always include schema instructions in the prompt regardless of these parameters.
     * 
     * @param modelName The name of the LLM model
     * @return Map of parameters to add to the request body
     */
    public static Map<String, Object> getJsonResponseParams(String modelName) {
        Map<String, Object> params = new HashMap<>();

        if (modelName == null) {
            return params;
        }

        String lowerModel = modelName.toLowerCase();

        // Determine provider type from model name
        Provider provider = identifyProvider(lowerModel);
        
        // Apply provider-specific JSON parameters
        switch (provider) {
            case OPENAI -> params.put("response_format", Map.of("type", "json_object"));
            case AZURE -> params.put("response_format", Map.of("type", "json_object"));
            case MISTRAL -> params.put("response_format", Map.of("type", "json_object"));
            case GROQ -> params.put("response_format", Map.of("type", "json_object"));
            case OLLAMA -> params.put("format", "json");
            case UNKNOWN, CLAUDE, COHERE, GEMINI, LLAMA -> { /* No params needed */ }
        }

        return params;
    }
    
    /**
     * Identifies the provider based on the model name
     * 
     * @param lowerModel The lowercase model name
     * @return The identified provider
     */
    private static Provider identifyProvider(String lowerModel) {
        if (lowerModel.contains("gpt-") || lowerModel.contains("text-") || lowerModel.contains("openai")) {
            return Provider.OPENAI;
        } else if (lowerModel.contains("azure")) {
            return Provider.AZURE;
        } else if (lowerModel.contains("mistral") || lowerModel.contains("mixtral")) {
            return Provider.MISTRAL;
        } else if (lowerModel.contains("groq")) {
            return Provider.GROQ;
        } else if (lowerModel.contains("ollama")) {
            return Provider.OLLAMA;
        } else if ((lowerModel.contains("llama") && !lowerModel.contains("ollama"))) {
            return Provider.LLAMA;
        } else if (lowerModel.contains("claude")) {
            return Provider.CLAUDE;
        } else if (lowerModel.contains("cohere")) {
            return Provider.COHERE;
        } else if (lowerModel.contains("gemini") || lowerModel.contains("vertex")) {
            return Provider.GEMINI;
        } else {
            return Provider.UNKNOWN;
        }
    }
    
    /**
     * Enum representing different LLM providers
     */
    private enum Provider {
        OPENAI,
        AZURE,
        MISTRAL,
        GROQ,
        OLLAMA,
        CLAUDE,
        COHERE,
        GEMINI,
        LLAMA,
        UNKNOWN
    }
}