package io.github.mingzilla.llmclient;

import java.util.function.Supplier;

import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Main client class for LLM operations
 * Handles all communication with the LLM API
 */
public class LlmClient {
    private final WebClient webClient;

    /**
     * Creates a new LlmClient with the specified WebClient
     * 
     * @param webClient The WebClient to use for HTTP requests
     */
    public LlmClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Creates a new LlmClient with a custom WebClient
     * 
     * @param webClient The WebClient to use
     * @return A new LlmClient
     */
    public static LlmClient create(WebClient webClient) {
        return new LlmClient(webClient);
    }

    /**
     * Handles verification and sending a request with a simpler API
     * Executes verification check before proceeding with the request
     * 
     * @param verificationSupplier A supplier that returns LlmClientError if
     *                             verification fails, null if successful
     * @param inputSupplier        A supplier function that provides the
     *                             LlmClientInput
     * @return A Mono that emits a ResponseEntity containing the LlmClientOutput when the request completes
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
     * @return A Mono that emits a ResponseEntity containing the LlmClientOutput when the request completes
     */
    public Mono<ResponseEntity<LlmClientOutput>> handleSend(Supplier<LlmClientInput> inputSupplier) {
        return Mono.fromCallable(inputSupplier::get)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(this::send);
    }

    /**
     * IMPORTANT: Do not use this method directly. Use handleSend() instead
     * to ensure proper handling of potentially blocking preparation code.
     * 
     * Sends a request to the LLM API and returns a single non-streaming response
     * 
     * @param input The LlmClientInput containing the request details
     * @return A Mono that emits a ResponseEntity containing the LlmClientOutput when the request completes
     */
    private Mono<ResponseEntity<LlmClientOutput>> send(LlmClientInput input) {
        return webClient.post()
                .uri(input.url())
                .bodyValue(input.body())
                .headers(input::setHeaders)
                .exchangeToMono(response -> {
                    return response.bodyToMono(String.class)
                            .map(body -> LlmClientOutput.fromResponse(response, body));
                })
                .onErrorResume(error -> {
                    if (error instanceof LlmClientPreflightException) {
                        return Mono.just(((LlmClientPreflightException) error).getOutput());
                    }
                    return Mono.just(LlmClientOutput.forError(LlmClientError.fromException(error)));
                })
                .map(output -> ResponseEntity.status(output.statusCode()).body(output));
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
                .flatMapMany(this::stream);
    }

    /**
     * IMPORTANT: Do not use this method directly. Use handleStream() instead
     * to ensure proper handling of potentially blocking preparation code.
     * 
     * Streams a request to the LLM API with JSON streaming format
     * 
     * @param input The LlmClientInput containing the request details
     * @return A Flux that emits each chunk from the streaming response
     */
    private Flux<LlmClientOutputChunk> stream(LlmClientInput input) {
        return webClient.post()
                .uri(input.url())
                .bodyValue(input.body())
                .headers(input::setHeaders)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.isEmpty())
                .map(LlmClientJsonUtil::parseStreamChunk)
                .takeUntil(LlmClientOutputChunk::done)
                .onErrorResume(error -> {
                    if (error instanceof LlmClientPreflightException) {
                        return Flux.just(LlmClientOutputChunk.forError(
                                ((LlmClientPreflightException) error).getOutput().error()));
                    }
                    return Flux.just(LlmClientOutputChunk.forError(LlmClientError.fromException(error)));
                });
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
                .flatMapMany(this::streamSse);
    }

    /**
     * IMPORTANT: Do not use this method directly. Use handleStreamSse() instead
     * to ensure proper handling of potentially blocking preparation code.
     * 
     * Streams a request to the LLM API with SSE streaming format
     * 
     * @param input The LlmClientInput containing the request details
     * @return A Flux that emits each SSE event from the streaming response
     */
    private Flux<ServerSentEvent<?>> streamSse(LlmClientInput input) {
        return webClient.post()
                .uri(input.url())
                .bodyValue(input.body())
                .headers(input::setHeaders)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.isEmpty())
                .map(line -> {
                    String data = line.startsWith("data: ") ? line.substring(6) : line;
                    if ("[DONE]".equals(data)) {
                        return ServerSentEvent.<String>builder()
                                .data("[DONE]")
                                .build();
                    } else {
                        return ServerSentEvent.<LlmClientOutputChunk>builder()
                                .data(LlmClientJsonUtil.parseStreamChunk(data))
                                .build();
                    }
                })
                .takeUntil(event -> event.data() != null && "[DONE]".equals(event.data()))
                .onErrorResume(error -> {
                    if (error instanceof LlmClientPreflightException) {
                        LlmClientOutput output = ((LlmClientPreflightException) error).getOutput();
                        return Flux.just(ServerSentEvent.<LlmClientOutputChunk>builder()
                                .data(LlmClientOutputChunk.forError(output.error()))
                                .build());
                    }
                    return Flux.just(ServerSentEvent.<LlmClientOutputChunk>builder()
                            .data(LlmClientOutputChunk.forError(LlmClientError.fromException(error)))
                            .build());
                });
    }
}