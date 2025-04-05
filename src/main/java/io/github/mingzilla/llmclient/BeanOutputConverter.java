package io.github.mingzilla.llmclient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.ParameterizedTypeReference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;

/**
 * Converts LLM outputs to Java bean instances
 * Based on Spring AI's approach but simplified for direct use
 */
public class BeanOutputConverter<T> {
    private final Class<T> targetClass;
    private final JavaType targetType;
    private final SchemaGenerator schemaGenerator;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Pattern to match and extract JSON from markdown code blocks or other wrappers
    private static final Pattern JSON_EXTRACTION_PATTERN = Pattern
            .compile("(?:```(?:json)?\\s*)?([{\\[].*?[}\\]])(?:\\s*```)?", Pattern.DOTALL);

    /**
     * Creates a converter for a simple class
     * 
     * @param targetClass The target class type
     */
    public BeanOutputConverter(Class<T> targetClass) {
        super();
        this.targetClass = targetClass;
        this.targetType = objectMapper.constructType(targetClass);

        // Configure schema generator with default settings
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2019_09, null);
        SchemaGeneratorConfig config = configBuilder.build();
        this.schemaGenerator = new SchemaGenerator(config);
    }

    /**
     * Creates a converter for generic types
     * 
     * @param typeReference The parameterized type reference
     */
    public BeanOutputConverter(ParameterizedTypeReference<T> typeReference) {
        super();
        this.targetClass = null;
        this.targetType = objectMapper.constructType(typeReference.getType());

        // Configure schema generator with default settings
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2019_09, null);
        SchemaGeneratorConfig config = configBuilder.build();
        this.schemaGenerator = new SchemaGenerator(config);
    }

    public String getFormatInstructions() {
        try {
            JsonNode schema = schemaGenerator.generateSchema(targetType);

            return String.format("""
                    Your response should be in JSON format.
                    The JSON should match this schema: %s
                    Do not include any explanations, only provide a RFC8259 compliant JSON response.
                    """, objectMapper.writeValueAsString(schema));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate schema for " +
                    (targetClass != null ? targetClass.getSimpleName() : targetType.toString()), e);
        }
    }

    public T convert(String input) {
        if (input == null || input.isBlank()) {
            throw new RuntimeException("Empty input cannot be converted");
        }

        try {
            // Extract JSON from potential wrappers (markdown, code blocks, etc.)
            String jsonContent = extractJsonContent(input);

            // Parse the JSON to the target type
            return objectMapper.readValue(jsonContent, targetType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert response to " +
                    (targetClass != null ? targetClass.getSimpleName() : targetType.toString()), e);
        }
    }

    /**
     * Extracts JSON content from the response, removing any markdown code blocks,
     * thinking tags, or other non-JSON content.
     * 
     * @param input The raw response from the LLM
     * @return Clean JSON content
     */
    private String extractJsonContent(String input) {
        // First try to extract JSON using regex pattern
        Matcher matcher = JSON_EXTRACTION_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // If no match found, try to clean the input manually
        String cleaned = input
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .replaceAll("<think>.*?</think>", "")
                .trim();

        // Basic validation that it looks like JSON
        if ((cleaned.startsWith("{") && cleaned.endsWith("}")) ||
                (cleaned.startsWith("[") && cleaned.endsWith("]"))) {
            return cleaned;
        }

        // If we couldn't extract valid JSON, return the original input
        // and let the converter handle potential errors
        return input;
    }
}