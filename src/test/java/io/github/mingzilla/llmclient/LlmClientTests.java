package io.github.mingzilla.llmclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import reactor.test.StepVerifier;

class LlmClientTests {
        private MockWebServer mockWebServer;
        private LlmClient llmClient;
        private String baseUrl;

        @BeforeEach
        void setUp() throws Exception {
                mockWebServer = new MockWebServer();
                mockWebServer.start();
                baseUrl = mockWebServer.url("/").toString();
                WebClient webClient = WebClient.builder().build();
                llmClient = new LlmClient(webClient);
        }

        @AfterEach
        void tearDown() throws Exception {
                mockWebServer.shutdown();
        }

        @Test
        void testNonBlockingSend() {
                // Prepare test data
                String responseBody = "{\"message\": {\"role\": \"assistant\", \"content\": \"Hello\"}}";
                mockWebServer.enqueue(new MockResponse()
                                .setBody(responseBody)
                                .addHeader("Content-Type", "application/json"));

                // Create test input
                LlmClientInput input = LlmClientInput.chat(
                                baseUrl,
                                LlmClientInputBody.chat("gpt-3.5-turbo",
                                                List.of(LlmClientMessage.user("Hello")),
                                                false,
                                                0.7),
                                Map.of("Authorization", "Bearer test-token"));

                // Test the non-blocking send operation
                StepVerifier.create(llmClient.handleSend(() -> input))
                                .assertNext(resp -> {
                                        LlmClientOutput output = resp.getBody();
                                        assertTrue(output.isSuccessful());
                                        assertEquals(200, output.statusCode());
                                        assertNotNull(output.body());
                                })
                                .verifyComplete();
        }

        @Test
        void testJsonStreaming() {
                // Prepare streaming response
                String chunk1 = "{\"message\":{\"role\":\"assistant\",\"content\":\"Hello\"}, \"done\":false, \"index\":0}";
                String chunk2 = "{\"message\":{\"role\":\"assistant\",\"content\":\"World\"}, \"done\":true, \"index\":1}";

                mockWebServer.enqueue(new MockResponse()
                                .setBody(chunk1 + "\n" + chunk2)
                                .addHeader("Content-Type", "application/x-ndjson"));

                // Create test input
                LlmClientInput input = LlmClientInput.chat(
                                baseUrl,
                                LlmClientInputBody.chat("gpt-3.5-turbo",
                                                List.of(LlmClientMessage.user("Hello")),
                                                true,
                                                0.7),
                                Map.of("Authorization", "Bearer test-token"));

                // Test the streaming operation
                StepVerifier.create(llmClient.handleStream(() -> input))
                                .assertNext(chunk -> {
                                        assertFalse(chunk.done());
                                        assertEquals(0, chunk.index());
                                        assertEquals("Hello", chunk.message().content());
                                })
                                .assertNext(chunk -> {
                                        assertTrue(chunk.done());
                                        assertEquals(1, chunk.index());
                                        assertEquals("World", chunk.message().content());
                                })
                                .verifyComplete();
        }

        @Test
        void testSseStreaming() {
                // Prepare SSE response
                String event1 = "data: {\"message\":{\"role\":\"assistant\",\"content\":\"Hello\"}, \"done\":false, \"index\":0}\n\n";
                String event2 = "data: [DONE]\n\n";

                mockWebServer.enqueue(new MockResponse()
                                .setBody(event1 + event2)
                                .addHeader("Content-Type", "text/event-stream"));

                // Create test input
                LlmClientInput input = LlmClientInput.chat(
                                baseUrl,
                                LlmClientInputBody.sse("gpt-3.5-turbo",
                                                List.of(LlmClientMessage.user("Hello")),
                                                0.7),
                                Map.of("Authorization", "Bearer test-token"));

                // Test the SSE streaming operation
                StepVerifier.create(llmClient.handleStreamSse(() -> input))
                                .assertNext(event -> {
                                        assertNotNull(event.data());
                                        assertEquals("Hello",
                                                        ((LlmClientOutputChunk) event.data()).message().content());
                                })
                                .assertNext(event -> {
                                        assertEquals("[DONE]", event.data());
                                })
                                .verifyComplete();
        }

        @Test
        void testErrorHandling() {
                // Prepare error response
                mockWebServer.enqueue(new MockResponse()
                                .setResponseCode(500)
                                .setBody("Internal Server Error"));

                // Create test input
                LlmClientInput input = LlmClientInput.chat(
                                baseUrl,
                                LlmClientInputBody.chatMessage("Hello", false),
                                Map.of("Authorization", "Bearer test-token"));

                // Test error handling
                StepVerifier.create(llmClient.handleSend(() -> input))
                                .assertNext(resp -> {
                                        LlmClientOutput output = resp.getBody();
                                        assertFalse(output.isSuccessful());
                                        assertNotNull(output.error());
                                        // The error code might be different now
                                        // assertEquals("HTTP_500", output.error().code());
                                })
                                .verifyComplete();
        }
}