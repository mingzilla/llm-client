package io.github.mingzilla.llmclient;

import java.util.function.Supplier;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Main client class for LLM operations
 * Handles all communication with the LLM API using a provider implementation
 */
public class LlmClient {
    private final LlmClientProvider provider;

    /**
     * Creates a new LlmClient with the specified provider
     * 
     * @param provider The LlmClientProvider to use for API requests
     */
    public LlmClient(LlmClientProvider provider) {
        this.provider = provider;
    }

    /**
     * Creates a new LlmClient with a WebClient provider
     * 
     * @param webClient The WebClient to use
     * @return A new LlmClient with a WebClientLlmProvider
     */
    public static LlmClient createWithWebClient(WebClient webClient) {
        return new LlmClient(new WebClientLlmProvider(webClient));
    }

    /**
     * Creates a new LlmClient with a Spring AI provider
     * 
     * @param chatClient The ChatClient to use for both streaming and non-streaming
     *                   requests
     * @return A new LlmClient with a SpringAiLlmProvider
     */
    public static LlmClient createWithSpringAi(ChatClient chatClient) {
        return new LlmClient(new SpringAiLlmProvider(chatClient));
    }

    /**
     * Handles verification and sending a request with a simpler API
     * Executes verification check before proceeding with the request
     * 
     * @param verificationSupplier A supplier that returns LlmClientError if
     *                             verification fails, null if successful
     * @param inputSupplier        A supplier function that provides the
     *                             LlmClientInput
     * @return A Mono that emits a ResponseEntity containing the LlmClientOutput
     *         when the request completes
     */
    public Mono<ResponseEntity<LlmClientOutput>> verifyAndSend(
            Supplier<LlmClientOutput> verificationSupplier,
            Supplier<LlmClientInput> inputSupplier) {
        LlmClientVerifier.require(verificationSupplier, "Verification supplier");
        return Mono.fromCallable(verificationSupplier::get)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(verificationOutput -> {
                    LlmClientVerifier.require(verificationOutput, "Verification result");
                    if (!verificationOutput.isSuccessful()) {
                        ResponseEntity<LlmClientOutput> resp = ResponseEntity.status(verificationOutput.statusCode())
                                .body(verificationOutput);
                        return Mono.just(resp);
                    }
                    return handleSend(inputSupplier);
                });
    }

    /**
     * Safely handles sending a request with potentially blocking preparation logic
     * This method should be used instead of send() to ensure proper reactive
     * patterns
     * 
     * @param inputSupplier A supplier function that provides the LlmClientInput,
     *                      may contain blocking code
     * @return A Mono that emits a ResponseEntity containing the LlmClientOutput
     *         when the request completes
     */
    public Mono<ResponseEntity<LlmClientOutput>> handleSend(Supplier<LlmClientInput> inputSupplier) {
        return Mono.fromCallable(inputSupplier::get)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(provider::send);
    }

    /**
     * Handles verification and streaming a request with a simpler API
     * Executes verification check before proceeding with the request
     * 
     * @param verificationSupplier A supplier that returns LlmClientError if
     *                             verification fails, null if successful
     * @param inputSupplier        A supplier function that provides the
     *                             LlmClientInput
     * @return A Flux that emits each chunk from the streaming response
     */
    public Flux<LlmClientOutputChunk> verifyAndStream(
            Supplier<LlmClientOutput> verificationSupplier,
            Supplier<LlmClientInput> inputSupplier) {
        LlmClientVerifier.require(verificationSupplier, "Verification supplier");
        return Mono.fromCallable(verificationSupplier::get)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(verificationOutput -> {
                    LlmClientVerifier.require(verificationOutput, "Verification result");
                    if (!verificationOutput.isSuccessful()) {
                        return Flux.just(LlmClientOutputChunk.forError(verificationOutput.error()));
                    }
                    return handleStream(inputSupplier);
                });
    }

    /**
     * Safely handles streaming a request with potentially blocking preparation
     * logic
     * This method should be used instead of stream() to ensure proper reactive
     * patterns
     * 
     * @param inputSupplier A supplier function that provides the LlmClientInput,
     *                      may contain blocking code
     * @return A Flux that emits each chunk from the streaming response
     */
    public Flux<LlmClientOutputChunk> handleStream(Supplier<LlmClientInput> inputSupplier) {
        return Mono.fromCallable(inputSupplier::get)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(provider::stream);
    }

    /**
     * Handles verification and SSE streaming a request with a simpler API
     * Executes verification check before proceeding with the request
     * 
     * @param verificationSupplier A supplier that returns LlmClientError if
     *                             verification fails, null if successful
     * @param inputSupplier        A supplier function that provides the
     *                             LlmClientInput
     * @return A Flux that emits each SSE event from the streaming response
     */
    public Flux<ServerSentEvent<?>> verifyAndStreamSse(
            Supplier<LlmClientOutput> verificationSupplier,
            Supplier<LlmClientInput> inputSupplier) {
        LlmClientVerifier.require(verificationSupplier, "Verification supplier");
        return Mono.fromCallable(verificationSupplier::get)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(verificationOutput -> {
                    LlmClientVerifier.require(verificationOutput, "Verification result");
                    if (!verificationOutput.isSuccessful()) {
                        return Flux.just(ServerSentEvent.builder()
                                .data(LlmClientOutputChunk.forError(verificationOutput.error()))
                                .build());
                    }
                    return handleStreamSse(inputSupplier);
                });
    }

    /**
     * Safely handles SSE streaming a request with potentially blocking preparation
     * logic
     * This method should be used instead of streamSse() to ensure proper reactive
     * patterns
     * 
     * @param inputSupplier A supplier function that provides the LlmClientInput,
     *                      may contain blocking code
     * @return A Flux that emits each SSE event from the streaming response
     */
    public Flux<ServerSentEvent<?>> handleStreamSse(Supplier<LlmClientInput> inputSupplier) {
        return Mono.fromCallable(inputSupplier::get)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(provider::streamSse);
    }
}