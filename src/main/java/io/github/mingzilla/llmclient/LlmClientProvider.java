package io.github.mingzilla.llmclient;

import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Provider interface for LLM client operations.
 * Implementations can use different backend technologies while maintaining the
 * same interface.
 */
public interface LlmClientProvider {

    /**
     * Sends a request to the LLM API and returns a single non-streaming response
     * 
     * @param input The LlmClientInput containing the request details
     * @return A Mono that emits a ResponseEntity containing the LlmClientOutput
     *         when the request completes
     */
    Mono<ResponseEntity<LlmClientOutput>> send(LlmClientInput input);

    /**
     * Streams a request to the LLM API with JSON streaming format
     * 
     * @param input The LlmClientInput containing the request details
     * @return A Flux that emits each chunk from the streaming response
     */
    Flux<LlmClientOutputChunk> stream(LlmClientInput input);

    /**
     * Streams a request to the LLM API with SSE streaming format
     * 
     * @param input The LlmClientInput containing the request details
     * @return A Flux that emits each SSE event from the streaming response
     */
    Flux<ServerSentEvent<?>> streamSse(LlmClientInput input);
}