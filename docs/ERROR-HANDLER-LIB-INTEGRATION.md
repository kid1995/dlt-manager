# Error Handler Library Integration Guide

How to integrate the DLT (Dead Letter Topic) error handling library into a Spring Boot service.

---

## System Architecture

```
Your Service (Kafka Consumer)
    │ message processing fails
    ▼
error-handler-lib:
├── ExceptionRecoverabilityChecker evaluates error type
│   ├── Recoverable (503, 408, 504, 502) → RetryTopicProducer
│   │   └── Retry Consumer (max-attempts = 10)
│   │       ├── Success → done
│   │       └── Still failing → RetryErrorHandler → DLT
│   └── Non-recoverable (400, 401, etc.) → DltTopicProducer
│       └── Creates CloudEvent + DltEventData
│           ▼
Kafka Topic: elpa-dlt (shared dead letter topic)
    ▼
dlt-manager backend:
├── Persists to PostgreSQL
└── REST API + Angular UI (view, retry, delete)
```

---

## Key Components

| Class | Package | Role |
|-------|---------|------|
| `@EnableErrorHandler` | `de.signaliduna.elpa.dltmanager` | Annotation to auto-configure the library |
| `ErrorHandler` | `adapter.message.errorhandler` | Primary error handler — routes to retry or DLT |
| `RetryErrorHandler` | `adapter.message.errorhandler` | Handles retry-topic failures — routes to DLT |
| `DltTopicProducer` | `adapter.message.producer` | Creates CloudEvent and publishes to DLT topic |
| `RetryTopicProducer` | `adapter.message.producer` | Publishes original message to retry topic |
| `ExceptionRecoverabilityChecker` | `adapter.message.errorhandler.checker` | Interface — decides if an exception is recoverable |
| `FeignExceptionRecoverabilityChecker` | `adapter.message.errorhandler.checker.http` | Checks Feign HTTP status codes |
| `VorgangProcessIdExtractor` | `adapter.message.errorhandler` | Extracts business process ID for logging |

---

## Retry Flow

```
Original Topic (max-attempts: 3, backoff: 500ms * 2)
    ▼
Attempt 1 → fails
Attempt 2 → fails (wait 500ms)
Attempt 3 → fails (wait 1000ms)
    ▼
ErrorHandler checks exception:
├── Recoverable? → Retry Topic (max-attempts: 10, backoff: 500ms * 2, max 60s)
│   ├── Attempt 1..10 with exponential backoff
│   ├── Success at any point → done
│   └── All 10 fail → RetryErrorHandler → DLT Topic
└── Non-recoverable? → DLT Topic directly
```

**Default recoverable HTTP codes:** 503, 408, 504, 502

---

## Step-by-Step Integration

### 1. Add Dependency

**build.gradle:**

```gradle
dependencies {
    implementation 'de.signaliduna.elpa:error-handler-lib:1.2.0'
}
```

### 2. Enable on Application Class

```java
@SpringBootApplication
@EnableErrorHandler
public class MyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyServiceApplication.class, args);
    }
}
```

`@EnableErrorHandler` auto-configures:
- `CloudEventMessageConverterConfiguration`
- `RecoverableHttpErrorCodes`
- `FeignExceptionRecoverabilityChecker`
- `VorgangProcessIdExtractor`

### 3. Define Kafka Topics

**application.yml:**

```yaml
topics:
  applicationReceived: elpa-application-received
  applicationReceivedRetry: elpa-application-received-retry
  elpa-dlt: elpa-dlt
```

**Naming convention:** `elpa-{service-name}-{event-type}` with `-retry` suffix for retry topics. The DLT topic `elpa-dlt` is shared across all services.

### 4. Configure Spring Cloud Stream Bindings

**application.yml:**

```yaml
spring:
  cloud:
    function:
      definition: onApplicationReceived
    stream:
      default:
        group: ${KAFKA_CONSUMER_GROUP:my-service}
      bindings:
        # Original topic consumer
        onApplicationReceived-in-0:
          destination: ${topics.applicationReceived}
          consumer:
            max-attempts: 3
            back-off-initial-interval: 500
            back-off-multiplier: 2
            back-off-max-interval: 5000
          error-handler-definition: onApplicationReceivedIn0ErrorHandler

        # Retry topic producer
        applicationReceivedRetry-out-0:
          destination: ${topics.applicationReceivedRetry}

        # Retry topic consumer
        applicationReceivedRetry-in-0:
          destination: ${topics.applicationReceivedRetry}
          consumer:
            max-attempts: 10
            back-off-initial-interval: 500
            back-off-multiplier: 2
            back-off-max-interval: 60000
          error-handler-definition: onApplicationReceivedRetryErrorHandler

        # DLT topic producer
        elpaDlt-out-0:
          destination: ${topics.elpa-dlt}

      kafka:
        binder:
          brokers: ${KAFKA_BROKER:localhost:9092}
          enableObservation: true
          configuration:
            security:
              protocol: ${KAFKA_SECURITY_PROTOCOL:PLAINTEXT}
            sasl:
              jaas:
                enabled: ${KAFKA_JAAS_ENABLED:false}
              mechanism: ${KAFKA_SASL_MECHANISM:PLAIN}
```

**4 bindings required:**

| Binding | Type | Purpose |
|---------|------|---------|
| `onApplicationReceived-in-0` | Input | Consumes from original topic |
| `applicationReceivedRetry-out-0` | Output | Produces to retry topic |
| `applicationReceivedRetry-in-0` | Input | Consumes from retry topic |
| `elpaDlt-out-0` | Output | Produces to DLT topic |

### 5. Create Error Handler Configuration

**ConsumerErrorHandlerConfig.java:**

```java
@Configuration
public class ConsumerErrorHandlerConfig {
    private static final String RETRY_BINDING = "applicationReceivedRetry-out-0";
    private static final String DLT_BINDING = "elpaDlt-out-0";

    private final StreamBridge streamBridge;
    private final BindingServiceProperties bindingServiceProperties;
    private final VorgangProcessIdExtractor processIdExtractor;

    public ConsumerErrorHandlerConfig(
        StreamBridge streamBridge,
        BindingServiceProperties bindingServiceProperties,
        VorgangProcessIdExtractor processIdExtractor
    ) {
        this.streamBridge = streamBridge;
        this.bindingServiceProperties = bindingServiceProperties;
        this.processIdExtractor = processIdExtractor;
    }

    @Bean
    public RetryTopicProducer retryTopicProducer() {
        String topicName = bindingServiceProperties
            .getBindingDestination(RETRY_BINDING);
        return new RetryTopicProducer(streamBridge, RETRY_BINDING, topicName);
    }

    @Bean
    public DltTopicProducer dltTopicProducer(Tracer tracer, JsonMapper jsonMapper) {
        String topicName = bindingServiceProperties
            .getBindingDestination(DLT_BINDING);
        DltTopicProducer.Props props = new DltTopicProducer.Props(
            "my-service",                                               // serviceName
            DLT_BINDING,                                                // binding
            topicName,                                                  // topic
            URI.create("/signal-iduna/elpa/my-service"),               // CloudEvent source
            "de.signaliduna.elpa.myservice.stream.onApplicationReceived-in.error"  // CloudEvent type
        );
        return new DltTopicProducer(streamBridge, tracer, jsonMapper, props);
    }

    @Bean
    public Consumer<ErrorMessage> onApplicationReceivedIn0ErrorHandler(
        RetryTopicProducer retryTopicProducer,
        DltTopicProducer dltTopicProducer
    ) {
        ExceptionRecoverabilityChecker checker =
            new FeignExceptionRecoverabilityChecker();
        return new ErrorHandler(
            retryTopicProducer, dltTopicProducer,
            processIdExtractor, checker
        );
    }

    @Bean
    public Consumer<ErrorMessage> onApplicationReceivedRetryErrorHandler(
        DltTopicProducer dltTopicProducer
    ) {
        return new RetryErrorHandler(dltTopicProducer, processIdExtractor);
    }
}
```

### 6. Create Consumer Function

```java
@Service
public class ApplicationReceivedConsumer {

    @Bean
    public Consumer<ApplicationMessage> onApplicationReceived() {
        return this::process;
    }

    private void process(ApplicationMessage message) {
        // Your business logic here.
        // DO NOT catch exceptions — let Spring Cloud Stream handle them.
        // Thrown exceptions trigger the error handler chain.

        validateMessage(message);
        callExternalService(message);  // FeignException → caught by error handler
        persistToDatabase(message);
    }
}
```

---

## What You Must Implement Yourself

| Bean/Function | Why |
|---------------|-----|
| `Consumer<T> onXxx()` | Your business logic — the consumer function |
| `ConsumerErrorHandlerConfig` | Wires the 4 beans (retry producer, DLT producer, 2 error handlers) |
| Custom `ExceptionRecoverabilityChecker` (optional) | If you have domain-specific recoverable exceptions |

## What the Library Provides

| Bean | Provided by |
|------|-------------|
| `RecoverableHttpErrorCodes` | `@EnableErrorHandler` |
| `FeignExceptionRecoverabilityChecker` | `@EnableErrorHandler` |
| `VorgangProcessIdExtractor` | `@EnableErrorHandler` |
| `CloudEventMessageConverterConfiguration` | `@EnableErrorHandler` |
| `ErrorHandler` | You instantiate in config with the producers and checker |
| `RetryErrorHandler` | You instantiate in config with the DLT producer |
| `RetryTopicProducer` | You instantiate in config with StreamBridge |
| `DltTopicProducer` | You instantiate in config with StreamBridge + Tracer |

---

## Kafka Topics to Create

| Topic | Created by | Consumer |
|-------|-----------|----------|
| `elpa-{service}-{event}` | Your service or ops | Your service |
| `elpa-{service}-{event}-retry` | Your service or ops | Your service |
| `elpa-dlt` | Already exists (shared) | dlt-manager backend |

---

## Custom Exception Handling

To make your own exceptions recoverable:

```java
ExceptionRecoverabilityChecker checker = ExceptionRecoverabilityChecker.combined(
    new FeignExceptionRecoverabilityChecker(),
    ExceptionRecoverabilityChecker.of(
        MyServiceException.class,
        ex -> ex.getStatusCode() >= 500
    )
);
```

To override the default recoverable HTTP codes:

```java
@Bean
public RecoverableHttpErrorCodes recoverableHttpErrorCodes() {
    return new RecoverableHttpErrorCodes(Set.of(503, 408, 504, 502, 429));
}
```

---

## DLT Event Data Structure

When an error reaches the DLT topic, a CloudEvent is published:

```json
{
  "id": "uuid",
  "type": "de.signaliduna.elpa.myservice.stream.onApplicationReceived-in.error",
  "source": "/signal-iduna/elpa/my-service",
  "time": "2024-07-10T11:10:36.818Z",
  "data": {
    "originalEventId": "kafka-message-id",
    "serviceName": "my-service",
    "addToDltTimestamp": "2024-07-10T11:10:36",
    "topic": "elpa-application-received",
    "partition": "0",
    "traceId": "opentelemetry-trace-id",
    "payload": "{...original message as JSON...}",
    "payloadMediaType": "application/json",
    "error": "Exception message",
    "stackTrace": "full stack trace"
  }
}
```

**PII safety:** `DltEventData.toString()` only outputs `originalEventId`, `serviceName`, `topic`, `addToDltTimestamp`. Payload and stack trace are never logged.

---

## Build Process Example (partnersync-style)

Based on real ELPA service integration patterns:

### File Structure

```
src/main/
├── java/de/signaliduna/elpa/myservice/
│   ├── MyServiceApplication.java              ← @EnableErrorHandler
│   ├── config/
│   │   └── ConsumerErrorHandlerConfig.java    ← 4 beans
│   └── adapter/
│       └── message/
│           └── ApplicationReceivedConsumer.java ← Consumer function
└── resources/
    ├── application.yml                         ← Topics + bindings
    └── application-local.yml                   ← Local Kafka config
```

### Build & Run

```bash
# Build
./gradlew build -x check

# Start infrastructure (Kafka, etc.)
cd /path/to/dev-labs
docker compose up kafka -d

# Run service
./gradlew bootRun

# Verify topics exist
docker exec -it lab-kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Verify Integration

1. Send a message to your input topic
2. Make the processing fail (e.g., external service down)
3. Check retry topic receives the message
4. After max retries, check `elpa-dlt` topic receives the CloudEvent
5. Open dlt-manager UI to see the failed event

---

## Checklist

- [ ] `error-handler-lib` dependency in `build.gradle`
- [ ] `@EnableErrorHandler` on application class
- [ ] 3 topic names defined in `application.yml`
- [ ] 4 bindings configured (original-in, retry-out, retry-in, dlt-out)
- [ ] `error-handler-definition` set on both input bindings
- [ ] `ConsumerErrorHandlerConfig` with `RetryTopicProducer`, `DltTopicProducer`, 2 error handler beans
- [ ] Consumer function matches `spring.cloud.function.definition`
- [ ] Binding names match exactly between YAML and Java
- [ ] `max-attempts`: 3 for original, 10 for retry
- [ ] Backoff configured (exponential, 500ms initial)
- [ ] Kafka topics created (`elpa-dlt` already exists)
- [ ] No PII in exception messages (they end up in DLT events)
