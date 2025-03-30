package io.github.mingzilla.llmclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LlmClientJsonUtilTests {

    @Test
    void extractErrorCode_nullOrEmpty_returnsNull() {
        assertNull(LlmClientJsonUtil.extractErrorCode(null));
        assertNull(LlmClientJsonUtil.extractErrorCode(""));
    }

    @Test
    void extractErrorCode_invalidJson_returnsNull() {
        assertNull(LlmClientJsonUtil.extractErrorCode("not json"));
    }

    @Test
    void extractErrorCode_directCode_returnsCode() {
        String json = "{\"code\":\"rate_limit_exceeded\",\"message\":\"Too many requests\"}";
        assertEquals("rate_limit_exceeded", LlmClientJsonUtil.extractErrorCode(json));
    }

    @Test
    void extractErrorCode_openAiStyle_returnsCode() {
        String json = "{\"error\":{\"code\":\"context_length_exceeded\",\"message\":\"Token limit exceeded\"}}";
        assertEquals("context_length_exceeded", LlmClientJsonUtil.extractErrorCode(json));
    }

    @Test
    void extractErrorCode_anthropicStyle_returnsType() {
        String json = "{\"error\":{\"type\":\"invalid_request_error\",\"message\":\"Invalid request\"}}";
        assertEquals("invalid_request_error", LlmClientJsonUtil.extractErrorCode(json));
    }

    @Test
    void extractErrorCode_noCodeOrType_returnsNull() {
        String json = "{\"error\":{\"message\":\"Something went wrong\"}}";
        assertNull(LlmClientJsonUtil.extractErrorCode(json));
    }
}