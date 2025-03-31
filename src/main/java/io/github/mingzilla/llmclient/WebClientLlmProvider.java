package io.github.mingzilla.llmclient;

import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * WebClient implementation of the LlmClientProvider interface.
 * This provider uses Spring WebFlux's WebClient to communicate with LLM APIs.
 */
public class WebClientLlmProvider implements LlmClientProvider {
    private final WebClient webClient;

    /**
     * Creates a new WebClientLlmProvider with the specified WebClient
     * 
     * @param webClient The WebClient to use for HTTP requests
     */
    public WebClientLlmProvider(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Sends a request to the LLM API and returns a single non-streaming response
     * 
     * @param input The LlmClientInput containing the request details
     * @return A Mono that emits a ResponseEntity containing the LlmClientOutput
     *         when the request completes
     */
    @Override
    public Mono<ResponseEntity<LlmClientOutput>> send(LlmClientInput input) {
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
     * Streams a request to the LLM API with JSON streaming format
     * 
     * @param input The LlmClientInput containing the request details
     * @return A Flux that emits each chunk from the streaming response
     */
    @Override
    public Flux<LlmClientOutputChunk> stream(LlmClientInput input) {
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
     * Streams a request to the LLM API with SSE streaming format
     * 
     * @param input The LlmClientInput containing the request details
     * @return A Flux that emits each SSE event from the streaming response
     */
    @Override
    public Flux<ServerSentEvent<?>> streamSse(LlmClientInput input) {
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