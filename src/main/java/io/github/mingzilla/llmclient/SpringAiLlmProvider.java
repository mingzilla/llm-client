package io.github.mingzilla.llmclient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.StreamResponseSpec;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring AI implementation of the LlmClientProvider interface.
 * This provider uses Spring AI's ChatClient to communicate with LLM APIs.
 */
public class SpringAiLlmProvider implements LlmClientProvider {
    private final ChatClient chatClient;

    /**
     * Creates a new SpringAiLlmProvider with the specified ChatClient
     * 
     * @param chatClient The ChatClient to use for both streaming and non-streaming
     *                   requests
     */
    public SpringAiLlmProvider(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Mono<ResponseEntity<LlmClientOutput>> send(LlmClientInput input) {
        try {
            // Convert LlmClientInput to Spring AI messages
            List<Message> messages = convertToSpringAiMessages(input.inputBody().messages());

            // Create request spec and set options
            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt();

            // Add messages to the request
            for (Message message : messages) {
                if (message instanceof SystemMessage systemMessage) {
                    requestSpec.system(systemMessage.getText());
                } else if (message instanceof UserMessage userMessage) {
                    requestSpec.user(userMessage.getText());
                } else if (message instanceof AssistantMessage assistantMessage) {
                    requestSpec.messages(assistantMessage);
                }
            }

            DefaultChatOptions options = new DefaultChatOptions();
            if (input.inputBody().temperature() != null) {
                options.setTemperature(input.inputBody().temperature());
            }
            if (input.inputBody().model() != null) {
                options.setModel(input.inputBody().model());
            }
            requestSpec.options(options);

            // Send the request
            return Mono.fromCallable(() -> {
                CallResponseSpec responseSpec = requestSpec.call();
                LlmClientMessage message = LlmClientMessage.assistant(responseSpec.content());
                LlmClientOutput output = new LlmClientOutput(
                        200, // Default success status
                        Map.of(), // Empty headers map
                        null, // No raw body needed
                        null, // No error
                        message // The message content
                );

                return ResponseEntity.ok(output);
            }).onErrorResume(error -> {
                LlmClientOutput errorOutput = LlmClientOutput.forError(
                        LlmClientError.fromException(error));
                return Mono.just(ResponseEntity.status(errorOutput.statusCode()).body(errorOutput));
            });
        } catch (Exception e) {
            LlmClientOutput errorOutput = LlmClientOutput.forError(
                    LlmClientError.fromException(e));
            return Mono.just(ResponseEntity.status(errorOutput.statusCode()).body(errorOutput));
        }
    }

    @Override
    public Flux<LlmClientOutputChunk> stream(LlmClientInput input) {
        try {
            // Convert LlmClientInput to Spring AI messages
            List<Message> messages = convertToSpringAiMessages(input.inputBody().messages());

            // Create request spec
            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt();

            // Add messages to the request
            for (Message message : messages) {
                if (message instanceof SystemMessage systemMessage) {
                    requestSpec.system(systemMessage.getText());
                } else if (message instanceof UserMessage userMessage) {
                    requestSpec.user(userMessage.getText());
                } else if (message instanceof AssistantMessage assistantMessage) {
                    requestSpec.messages(assistantMessage);
                }
            }

            DefaultChatOptions options = new DefaultChatOptions();
            if (input.inputBody().temperature() != null) {
                options.setTemperature(input.inputBody().temperature());
            }
            if (input.inputBody().model() != null) {
                options.setModel(input.inputBody().model());
            }
            requestSpec.options(options);

            // Stream response using ReactiveStreams
            AtomicInteger index = new AtomicInteger(0);
            StreamResponseSpec streamResponseSpec = requestSpec.stream();

            return streamResponseSpec.content()
                    .map(content -> {
                        return new LlmClientOutputChunk(
                                LlmClientMessage.assistant(content),
                                false, // Not done yet
                                index.getAndIncrement(),
                                null // No error
                        );
                    })
                    // Add a final "done" chunk
                    .concatWith(Mono.just(new LlmClientOutputChunk(
                            LlmClientMessage.assistant(""),
                            true, // This is the final chunk
                            index.get(),
                            null)))
                    .onErrorResume(error -> {
                        return Flux.just(LlmClientOutputChunk.forError(
                                LlmClientError.fromException(error)));
                    });
        } catch (Exception e) {
            return Flux.just(LlmClientOutputChunk.forError(
                    LlmClientError.fromException(e)));
        }
    }

    @Override
    public Flux<ServerSentEvent<?>> streamSse(LlmClientInput input) {
        try {
            // Convert LlmClientInput to Spring AI messages
            List<Message> messages = convertToSpringAiMessages(input.inputBody().messages());

            // Create request spec
            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt();

            // Add messages to the request
            for (Message message : messages) {
                if (message instanceof SystemMessage systemMessage) {
                    requestSpec.system(systemMessage.getText());
                } else if (message instanceof UserMessage userMessage) {
                    requestSpec.user(userMessage.getText());
                } else if (message instanceof AssistantMessage assistantMessage) {
                    requestSpec.messages(assistantMessage);
                }
            }

            DefaultChatOptions options = new DefaultChatOptions();
            if (input.inputBody().temperature() != null) {
                options.setTemperature(input.inputBody().temperature());
            }
            if (input.inputBody().model() != null) {
                options.setModel(input.inputBody().model());
            }
            requestSpec.options(options);

            // Stream response and convert to SSE
            AtomicInteger index = new AtomicInteger(0);
            StreamResponseSpec streamResponseSpec = requestSpec.stream();

            return streamResponseSpec.content()
                    .map(content -> {
                        LlmClientOutputChunk outputChunk = new LlmClientOutputChunk(
                                LlmClientMessage.assistant(content),
                                false, // Not done yet
                                index.getAndIncrement(),
                                null // No error
                        );

                        return ServerSentEvent.<LlmClientOutputChunk>builder()
                                .data(outputChunk)
                                .build();
                    }); // todo - error handling and done
            // .concatWith(Mono.just(ServerSentEvent.<String>builder().data("[DONE]").build()))
            // .onErrorResume(error -> {
            // LlmClientError llmError = LlmClientError.fromException(error);
            // return Flux.just(ServerSentEvent.<LlmClientOutputChunk>builder()
            // .data(LlmClientOutputChunk.forError(llmError))
            // .build());
            // });
        } catch (Exception e) {
            LlmClientError llmError = LlmClientError.fromException(e);
            return Flux.just(ServerSentEvent.<LlmClientOutputChunk>builder()
                    .data(LlmClientOutputChunk.forError(llmError))
                    .build());
        }
    }

    /**
     * Converts LlmClientMessages to Spring AI Messages
     */
    private List<Message> convertToSpringAiMessages(List<LlmClientMessage> messages) {
        return messages.stream()
                .map(this::convertToSpringAiMessage)
                .collect(Collectors.toList());
    }

    /**
     * Converts a single LlmClientMessage to a Spring AI Message
     */
    private Message convertToSpringAiMessage(LlmClientMessage message) {
        return switch (message.role()) {
            case "system" -> new SystemMessage(message.content());
            case "user" -> new UserMessage(message.content());
            case "assistant" -> new AssistantMessage(message.content());
            default -> throw new IllegalArgumentException("Unknown role: " + message.role());
        };
    }
}