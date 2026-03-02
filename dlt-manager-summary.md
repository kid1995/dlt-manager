# DLT Manager — Tổng quan hệ thống

## 1. Mục đích

DLT Manager là hệ thống quản lý **Dead Letter Topic (DLT)** cho các Kafka events trong hệ sinh thái elpa:4 tại Signal Iduna. Khi một microservice không thể xử lý Kafka message sau nhiều lần retry, message đó được chuyển vào DLT thay vì bị mất. DLT Manager thu thập, lưu trữ và cung cấp giao diện admin để xem, retry hoặc xóa các failed events.

Hệ thống gồm 2 subproject:

- **error-handler-lib** — Java library nhúng vào các service elpa:4, tự động phân loại lỗi và đẩy failed events vào retry topic hoặc DLT.
- **backend** — Spring Boot 4 application nhận DLT events từ Kafka, lưu vào MongoDB và cung cấp REST API cho Angular UI.

## 2. Kiến trúc tổng thể

```
┌─────────────────────────────────┐
│  Service A (z.B. partnersync)   │
│                                 │
│  Kafka Consumer nhận message    │
│         ↓ (xử lý thất bại)     │
│  ┌───────────────────────────┐  │
│  │  error-handler-lib        │  │
│  │                           │  │
│  │  ExceptionRecoverability  │  │
│  │  Checker quyết định:      │  │
│  │                           │  │
│  │  Recoverable (503, 408…)  │──────────► Kafka: retry-topic
│  │         │                 │  │              ↓
│  │  Non-recoverable (4xx…)   │  │         Consumer retry
│  │         │                 │  │              ↓ (vẫn thất bại)
│  │         ▼                 │  │         RetryErrorHandler
│  │  DltTopicProducer tạo     │  │              │
│  │  CloudEvent + DltEventData│  │              ▼
│  └───────────┬───────────────┘  │
│              │                  │
└──────────────┼──────────────────┘
               │
               ▼
        Kafka: elpa-dlt topic
               │
               ▼
┌──────────────────────────────────┐
│  backend (DLT Manager App)       │
│                                  │
│  DltEventConsumerAdapter         │
│  (Spring Cloud Stream Consumer)  │
│         ↓                        │
│  Deserialisiert CloudEvent       │
│  → DltEventData → DltEvent       │
│         ↓                        │
│  MongoDB (Persistenz)            │
│         ↓                        │
│  REST API (DltManagerController) │
│         ↓                        │
│  Angular UI (DLT Manager UI)    │
│  - Übersicht aller DLT Events   │
│  - Details (Payload, StackTrace) │
│  - Retry (Papierantrag resend)   │
│  - Löschen                       │
└──────────────────────────────────┘
```

## 3. error-handler-lib im Detail

### 3.1 Zweck

Die Library übernimmt die Fehlerbehandlung für Spring Cloud Stream Kafka Bindings. Wenn ein Consumer eine Message nach der konfigurierten `max-attempts`-Anzahl nicht verarbeiten kann, entscheidet die Library automatisch:

- **Recoverable Errors** (HTTP 503, 408, 504, 502) → Message wird an einen Retry-Topic gesendet, um später erneut verarbeitet zu werden.
- **Non-recoverable Errors** (alle anderen) → Ein CloudEvent mit vollständigem Fehlerkontext wird erstellt und an den DLT-Topic gesendet.

### 3.2 Kernkomponenten

| Komponente | Aufgabe |
|---|---|
| `ErrorHandler` | Primärer Error-Handler für Bindings. Entscheidet zwischen Retry und DLT. |
| `RetryErrorHandler` | Error-Handler für Retry-Bindings. Sendet bei erneutem Fehler immer an DLT. |
| `ExceptionRecoverabilityChecker` | Interface zur Klassifizierung von Exceptions als recoverable/non-recoverable. |
| `FeignExceptionRecoverabilityChecker` | Prüft HTTP-Statuscodes bei FeignExceptions. |
| `MultiExceptionRecoverabilityChecker` | Kombiniert mehrere Checker (OR-Logik). |
| `DltTopicProducer` | Erstellt CloudEvent mit DltEventData und sendet über StreamBridge. |
| `RetryTopicProducer` | Sendet Original-Payload an Retry-Topic über StreamBridge. |
| `VorgangProcessIdExtractor` | Extrahiert `processId` aus Vorgang-Payload für Logging. |
| `@EnableErrorHandler` | Aktivierungsannotation — importiert die notwendige Konfiguration. |

### 3.3 DltEventData — Aufbau des DLT-Payloads

Wenn ein Event in den DLT gesendet wird, wird ein `DltEventData`-Objekt erstellt, das den gesamten Fehlerkontext enthält:

```
DltEventData
├── originalEventId      ← ID der Original-Message aus Kafka-Headers
├── serviceName          ← Name des fehlgeschlagenen Services
├── addToDltTimestamp    ← Zeitpunkt der DLT-Einlieferung
├── topic                ← Kafka-Topic, auf dem die Message empfangen wurde
├── partition            ← Kafka-Partition
├── traceId              ← Trace-ID aus Micrometer Tracing (für Observability)
├── payload              ← Original-Payload als JSON-String
├── payloadMediaType     ← Content-Type (i.d.R. application/json)
├── error                ← Fehlermeldung der Exception
└── stackTrace           ← Vollständiger StackTrace
```

Der Payload wird als String gespeichert, damit der DLT Manager die konkreten Model-Klassen der Services nicht kennen muss.

### 3.4 Integration in einen Service

**Schritt 1: Dependency hinzufügen**

```groovy
// build.gradle
dependencies {
    implementation 'de.signaliduna.elpa.dlt-manager:error-handler-lib:<version>'
}
```

**Schritt 2: Annotation aktivieren**

```java
@SpringBootApplication
@EnableErrorHandler  // aktiviert die Library
public class MeinServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeinServiceApplication.class, args);
    }
}
```

**Schritt 3: Error-Handler und Producer Beans konfigurieren**

```java
@Configuration
public class ErrorHandlerConfiguration {

    @Bean
    public DltTopicProducer dltTopicProducer(
            StreamBridge streamBridge,
            Tracer tracer,
            JsonMapper jsonMapper
    ) {
        return new DltTopicProducer(
            streamBridge,
            tracer,
            jsonMapper,
            new DltTopicProducer.Props(
                "mein-service",                                              // serviceName
                "dltEventOut",                                               // Binding-Name für StreamBridge
                "elpa-dlt",                                                  // Kafka-Topic-Name
                URI.create("/signal-iduna/elpa/mein-service"),               // CloudEvent source
                "de.signaliduna.elpa.mein-service.stream.consumer-in.error"  // CloudEvent type
            )
        );
    }

    @Bean
    public RetryTopicProducer retryTopicProducer(StreamBridge streamBridge) {
        return new RetryTopicProducer(
            streamBridge,
            "retryEventOut",       // Binding-Name
            "mein-service-retry"   // Kafka-Topic-Name
        );
    }

    @Bean
    public ErrorHandler errorHandler(
            RetryTopicProducer retryTopicProducer,
            DltTopicProducer dltTopicProducer,
            VorgangProcessIdExtractor idExtractor,
            ExceptionRecoverabilityChecker recoverabilityChecker
    ) {
        return new ErrorHandler(
            retryTopicProducer,
            dltTopicProducer,
            idExtractor,
            recoverabilityChecker
        );
    }

    @Bean
    public RetryErrorHandler retryErrorHandler(
            DltTopicProducer dltTopicProducer,
            VorgangProcessIdExtractor idExtractor
    ) {
        return new RetryErrorHandler(dltTopicProducer, idExtractor);
    }
}
```

**Schritt 4: application.yml — Bindings konfigurieren**

```yaml
spring:
  cloud:
    stream:
      bindings:
        # Haupt-Consumer
        meinConsumer-in-0:
          destination: mein-topic
          group: mein-service
          error-handler-definition: errorHandler   # ← verweist auf Bean-Name

        # Retry-Consumer
        retryConsumer-in-0:
          destination: mein-service-retry
          group: mein-service-retry
          error-handler-definition: retryErrorHandler

        # DLT-Producer (StreamBridge)
        dltEventOut:
          destination: elpa-dlt

        # Retry-Producer (StreamBridge)
        retryEventOut:
          destination: mein-service-retry

      kafka:
        binder:
          brokers: ${KAFKA_BROKER}
        bindings:
          meinConsumer-in-0:
            consumer:
              max-attempts: 3    # Anzahl Versuche vor Error-Handler
          retryConsumer-in-0:
            consumer:
              max-attempts: 2
```

**Schritt 5 (optional): Eigenen ExceptionRecoverabilityChecker hinzufügen**

Falls neben FeignExceptions auch andere Exceptions als recoverable gelten sollen:

```java
@Bean
public ExceptionRecoverabilityChecker customRecoverabilityChecker(
        RecoverableHttpErrorCodes codes
) {
    ExceptionRecoverabilityChecker feignChecker =
        new FeignExceptionRecoverabilityChecker(codes);

    // Eigene Logik hinzufügen
    ExceptionRecoverabilityChecker customChecker =
        ex -> ex instanceof MyTemporaryException;

    return feignChecker.and(customChecker);  // OR-Verknüpfung
}
```

### 3.5 Ablauf im Fehlerfall

```
Consumer empfängt Message
        ↓
Verarbeitung schlägt fehl (Exception)
        ↓
Spring Cloud Stream wiederholt (max-attempts)
        ↓
Alle Versuche fehlgeschlagen
        ↓
error-handler-definition Bean wird aufgerufen
        ↓
ErrorHandler.accept(ErrorMessage)
        ↓
ExceptionRecoverabilityChecker.isRecoverable(exception)?
        ↓                              ↓
    JA (503, 408…)              NEIN (400, 500…)
        ↓                              ↓
RetryTopicProducer             DltTopicProducer
→ Original-Payload             → CloudEvent mit DltEventData
→ an retry-topic               → an elpa-dlt topic
        ↓                              ↓
Retry-Consumer                 DLT Manager Backend
        ↓ (erneut fehlgeschlagen)
RetryErrorHandler
        ↓
DltTopicProducer → an elpa-dlt topic
```

## 4. backend im Detail

### 4.1 Technologie-Stack

- Spring Boot 4 mit Spring Cloud Stream (Kafka Binder)
- MongoDB für Persistenz
- Spring Security mit OAuth2/OIDC (JWT-basiert)
- OpenFeign für Service-zu-Service-Kommunikation
- CloudEvents SDK für Kafka-Message-Format
- Jackson 3 (`tools.jackson.databind.json.JsonMapper`)
- springdoc-openapi für API-Dokumentation
- springwolf für AsyncAPI-Dokumentation

### 4.2 Kafka Consumer

`DltEventConsumerAdapter` empfängt CloudEvents vom Topic `elpa-dlt`:

```
Kafka Message (elpa-dlt)
    ↓
CloudEventMessageConverter (Headers → CloudEvent)
    ↓
Consumer<CloudEvent> dltEventReceived()
    ↓
CloudEvent.getData() → bytes → JsonMapper → DltEventData
    ↓
EventDataMapper.toDomainObject(cloudEvent, dltEventData)
    → CloudEvent.getId() wird zu dltEventId
    → DltEventData-Felder werden zu DltEvent-Feldern
    ↓
IncomingDltEventManager.onDltEvent()
    ↓
DltEventPersistenceAdapter.save() → MongoDB
```

### 4.3 REST API

Alle Endpunkte erfordern JWT-Authentifizierung und Autorisierung über eine Whitelist (`authorization.users`).

| Methode | Pfad | Beschreibung | Response |
|---|---|---|---|
| GET | `/api/events/overview` | Übersicht aller DLT-Events (ohne Payload/StackTrace) | `GetDltEventsResponse` |
| GET | `/api/events/overview/{id}` | Übersicht eines Events | `DltEventOverviewItemDto` |
| GET | `/api/events/details/{id}` | Vollständige Details inkl. Payload und StackTrace | `DltEventFullItemDto` |
| POST | `/api/events/re-processing/{id}` | Erneute Verarbeitung des Original-Events auslösen | 200 OK / 404 |
| DELETE | `/api/events/{id}` | DLT-Event aus der Datenbank löschen | 204 / 404 |

### 4.4 Retry-Logik (Reprocessing)

Der Retry-Ablauf in `DltEventAdminService.resendPapierantrag()`:

```
Admin klickt "Retry" in der UI
    ↓
POST /api/events/re-processing/{dltEventId}
    ↓
DltEvent aus MongoDB laden
    ↓
Payload als Vorgang deserialisieren
    ↓
metadaten.rohdatenUuid extrahieren
    ↓
PapierantragEingangClient.resendPapierantrag(rohdatenUuid) via Feign
    ↓                                    ↓
Erfolg                              FeignException
    ↓                                    ↓
lastAdminAction = TRIGGERED         lastAdminAction = FAILED
    ↓                                    ↓
MongoDB Update                      MongoDB Update
```

### 4.5 Security

Die Autorisierung basiert auf einer Whitelist von User-IDs:

```yaml
authorization:
  users: S000325, S000759, U116330, ...
```

`WebSecurityConfig` definiert einen Custom SpEL-Ausdruck `isAuthorizedUser()`, der den `uid`-Claim aus dem JWT gegen diese Liste prüft. Öffentliche Endpunkte (Swagger, Health, Metrics) sind via `permit-all` freigegeben.

### 4.6 Datenmodell

```
MongoDB Collection: "DltEvent"
┌──────────────────────────────────────┐
│ DltEventEntity                       │
├──────────────────────────────────────┤
│ dltEventId (PK)      : String       │ ← CloudEvent ID aus error-handler-lib
│ originalEventId       : String       │ ← ID der ursprünglichen Kafka-Message
│ serviceName           : String       │ ← welcher Service fehlgeschlagen ist
│ addToDltTimestamp     : LocalDateTime│
│ topic                 : String       │ ← Kafka-Topic
│ partition             : String       │
│ traceId               : String?     │ ← für Tracing/Observability
│ payload               : String       │ ← Original-Event als JSON-String
│ payloadMediaType      : String       │
│ error                 : String?     │ ← Fehlermeldung
│ stackTrace            : String?     │ ← vollständiger StackTrace
│ lastAdminAction       :             │ ← letzte Admin-Aktion
│   ├── userName        : String       │
│   ├── timestamp       : LocalDateTime│
│   ├── actionName      : String       │
│   ├── actionDetails   : String?     │
│   ├── status          : String       │ ← TRIGGERED / SUCCEEDED / FAILED
│   └── statusError     : String?     │
└──────────────────────────────────────┘
```

## 5. Bekannte Sicherheitsaspekte

- `DltEvent.toString()` ist überschrieben, um PII-Leaks in Logs zu vermeiden.
- `DltEventData` und andere Records haben kein `toString()`-Override — potentielles Risiko.
- `SecurityControllerAdvice` gibt `ex.getMessage()` an den Client zurück — kann interne Details enthalten.
- FeignException-Logs können Request/Response-Bodies mit personenbezogenen Daten enthalten.
- Detaillierte Empfehlungen zur Behebung sind separat dokumentiert.

## 6. Zusammenfassung

Das DLT Manager System bietet eine robuste Fehlerbehandlungskette für Kafka-basierte Microservices. Die `error-handler-lib` abstrahiert die Komplexität der Fehlerklassifizierung und DLT-Einlieferung, sodass Services lediglich die Library aktivieren und ihre Bindings konfigurieren müssen. Das Backend sammelt alle fehlgeschlagenen Events zentral und stellt sie über eine REST API und Angular-UI für administrative Aktionen bereit.
