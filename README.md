# LLM Client

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mingzilla/llm-client.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.mingzilla/llm-client)
[![License](https://img.shields.io/github/license/mingzilla/llm-client)](https://github.com/mingzilla/llm-client/blob/main/LICENSE)

A reactive Java client for LLM (Large Language Model) APIs with streaming support, following the specification described in [`streaming-api-spec`](https://mingzilla.github.io/specification/streaming-api-spec).

## Features

- Built on Spring WebFlux for non-blocking reactive operations
- Support for both regular and streaming API calls
- Server-Sent Events (SSE) streaming support
- JSON streaming capability
- Error handling with detailed error information
- Follows the standardized API format compatible with various LLM providers

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.mingzilla</groupId>
    <artifactId>llm-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.mingzilla:llm-client:1.0.0'
```

## Controller Example

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final LlmClient llmClient;
    private final AuthService authService;
    
    @Autowired
    public ChatController(LlmClient llmClient, AuthService authService) {
        this.llmClient = llmClient;
        this.authService = authService;
    }
    
    @PostMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<LlmClientOutput> json(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody LlmClientInputBody inputBody) {
        String clientToken = authHeader.replace("Bearer ", "");
        return llmClient.verifyAndSend(
            () -> {       
                boolean valid = authService.validateToken(clientToken);
                return valid ? LlmClientOutput.verificationSuccess() : LlmClientOutput.forError401();
            },
            () -> {
                Map<String, String> headers = authService.getProviderHeaders(clientToken);
                return LlmClientInput.chat("https://an-llm-provider/chat/json", inputBody, headers);
            }
        );
    }

    @PostMapping(value = "/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<LlmClientOutputChunk> stream(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody LlmClientInputBody inputBody) {
        String clientToken = authHeader.replace("Bearer ", "");
        return llmClient.verifyAndStream(
            () -> {
                boolean valid = authService.validateToken(clientToken);
                return valid ? LlmClientOutput.verificationSuccess() : LlmClientOutput.forError401();
            },
            () -> {
                Map<String, String> headers = authService.getProviderHeaders(clientToken);
                return LlmClientInput.chat("https://an-llm-provider/chat/stream", inputBody, headers);
            }
        );
    }

    @PostMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<?>> sse(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody LlmClientInputBody inputBody) {
        String clientToken = authHeader.replace("Bearer ", "");
        return llmClient.verifyAndStreamSse(
            () -> {
                boolean valid = authService.validateToken(clientToken);
                return valid ? LlmClientOutput.verificationSuccess() : LlmClientOutput.forError401();
            },
            () -> {
                Map<String, String> headers = authService.getProviderHeaders(clientToken);
                return LlmClientInput.chat("https://an-llm-provider/chat/sse", inputBody, headers);
            }
        );
    }
}
```

## Request Format

LLM API requests use `LlmClientInputBody` with this JSON structure:

```json
{
  "model": "model-name",
  "messages": [
    {
      "role": "system",
      "content": "You are a helpful assistant."
    },
    {
      "role": "user",
      "content": "Hello, how are you?"
    }
  ],
  "stream": false,
  "temperature": 0.7
}
```

## Spring Configuration

```java
@Configuration
public class LlmClientConfig {
    
    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
            .responseTimeout(Duration.ofSeconds(60));
            
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
    
    @Bean
    public LlmClient llmClient(WebClient webClient) {
        return LlmClient.create(webClient);
    }
}
```

## API Endpoints

Following the specification in [`streaming-api-spec.md`](./streaming-api-spec.md), this client works with these endpoints:

| Endpoint       | Method | Description                        | Return Type                  |
| -------------- | ------ | ---------------------------------- | ---------------------------- |
| `/chat/json`   | POST   | Regular non-streaming responses    | `Mono<LlmClientOutput>`      |
| `/chat/stream` | POST   | JSON streaming responses           | `Flux<LlmClientOutputChunk>` |
| `/chat/sse`    | POST   | SSE streaming (Server-Sent Events) | `Flux<ServerSentEvent<?>>`   |

## Design Principles

- **Fully Reactive**: Built entirely on Spring WebFlux for maximum performance and scalability
- **Non-blocking I/O**: All operations are non-blocking, from API calls to response handling
- **Thread Safety**: Safely handles blocking operations via `Schedulers.boundedElastic()`
- **Separation of Concerns**: Clear distinction between input, output, and processing logic
- **Error Handling**: Comprehensive error reporting with detailed information
- **Immutability**: Uses Java records for immutable data structures

## License

[MIT License](LICENSE)