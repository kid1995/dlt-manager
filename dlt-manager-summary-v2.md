# DLT Manager — Tổng quan hệ thống (v2 — PostgreSQL + PII Hardening)

## 1. Mục đích

DLT Manager là hệ thống quản lý **Dead Letter Topic (DLT)** cho các Kafka events trong hệ sinh thái elpa:4 tại Signal Iduna. Khi một microservice không thể xử lý Kafka message sau nhiều lần retry, message đó được chuyển vào DLT thay vì bị mất. DLT Manager thu thập, lưu trữ và cung cấp giao diện admin để xem, retry hoặc xóa các failed events.

Hệ thống gồm 2 subproject:

- **error-handler-lib** — Java library nhúng vào các service elpa:4, tự động phân loại lỗi và đẩy failed events vào retry topic hoặc DLT.
- **backend** — Spring Boot 4 application nhận DLT events từ Kafka, lưu vào PostgreSQL và cung cấp REST API cho Angular UI.

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
│  PostgreSQL (JPA + Flyway)       │
│  ├── dlt_event                   │
│  └── admin_action_history        │
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

| Komponente                            | Aufgabe                                                                     |
| ------------------------------------- | --------------------------------------------------------------------------- |
| `ErrorHandler`                        | Primärer Error-Handler für Bindings. Entscheidet zwischen Retry und DLT.    |
| `RetryErrorHandler`                   | Error-Handler für Retry-Bindings. Sendet bei erneutem Fehler immer an DLT.  |
| `ExceptionRecoverabilityChecker`      | Interface zur Klassifizierung von Exceptions (recoverable/non-recoverable). |
| `FeignExceptionRecoverabilityChecker` | Prüft HTTP-Statuscodes bei FeignExceptions.                                 |
| `MultiExceptionRecoverabilityChecker` | Kombiniert mehrere Checker (OR-Logik).                                      |
| `DltTopicProducer`                    | Erstellt CloudEvent mit DltEventData und sendet über StreamBridge.          |
| `RetryTopicProducer`                  | Sendet Original-Payload an Retry-Topic über StreamBridge.                   |
| `VorgangProcessIdExtractor`           | Extrahiert `processId` aus Vorgang-Payload für Logging.                     |
| `@EnableErrorHandler`                 | Aktivierungsannotation — importiert die notwendige Konfiguration.           |

### 3.3 DltEventData — Aufbau des DLT-Payloads

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

**PII-Schutz:** `DltEventData.toString()` gibt nur safe Metadaten aus (`originalEventId`, `serviceName`, `topic`, `addToDltTimestamp`). Payload, error und stackTrace werden nie im toString() ausgegeben.

### 3.4 Integration in einen Service

**Schritt 1:** Dependency hinzufügen (`implementation 'de.signaliduna.elpa.dlt-manager:error-handler-lib:<version>'`)

**Schritt 2:** `@EnableErrorHandler` auf der Application-Klasse

**Schritt 3:** Error-Handler und Producer Beans konfigurieren (`DltTopicProducer`, `RetryTopicProducer`, `ErrorHandler`, `RetryErrorHandler`)

**Schritt 4:** `application.yml` — Bindings mit `error-handler-definition` konfigurieren

**Schritt 5 (optional):** Eigenen `ExceptionRecoverabilityChecker` hinzufügen via `.and()`

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
ErrorHandler.accept(ErrorMessage)
        ↓
ExceptionRecoverabilityChecker.isRecoverable(exception)?
    ├── JA (503, 408…) → RetryTopicProducer → retry-topic → Retry-Consumer
    │                                                            ↓ (erneut fehlgeschlagen)
    │                                                     RetryErrorHandler → DLT
    └── NEIN (400, 500…) → DltTopicProducer → CloudEvent → elpa-dlt topic → DLT Manager Backend
```

## 4. backend im Detail

### 4.1 Technologie-Stack

| Technologie                                          | Einsatz                                                 |
| ---------------------------------------------------- | ------------------------------------------------------- |
| Spring Boot 4                                        | Application Framework                                   |
| Spring Cloud Stream (Kafka Binder)                   | Kafka Consumer                                          |
| PostgreSQL + JPA + Flyway                            | Persistenz (migriert von MongoDB)                       |
| Spring Security + OAuth2/OIDC (JWT)                  | Authentifizierung & Autorisierung                       |
| spring-addons-oidc                                   | Custom SpEL `isAuthorizedUser()`                        |
| OpenFeign                                            | Service-zu-Service-Kommunikation (papierantrag-eingang) |
| CloudEvents SDK                                      | Kafka-Message-Format                                    |
| Jackson 3 (`tools.jackson.databind.json.JsonMapper`) | JSON Serialisierung                                     |
| springdoc-openapi                                    | REST API Dokumentation                                  |
| springwolf                                           | AsyncAPI Dokumentation                                  |
| Testcontainers (PostgreSQL)                          | Integration Tests                                       |

### 4.2 Service-Architektur

```
DltManagerController
└── DltEventAdminService
    ├── DltEventPersistenceAdapter  ← findAll, findById, addAdminAction, delete
    ├── PapierantragEingangAdapter  ← Feign call für Retry
    └── JsonMapper                  ← Payload decode

IncomingDltEventManager             ← Kafka Consumer pipeline
└── DltEventPersistenceAdapter      ← save incoming events

SafeExceptionLogger                 ← PII-sichere Exception-Verarbeitung (Utility)
```

**Hinweis:** Zukünftig ist eine Aufteilung von `DltEventAdminService` geplant:

- `DltEventService` — reine CRUD-Operationen (findAll, findById, save)
- `DltEventAdminActionService` — Admin-Aktionen (resend, delete, getAvailableActions)

### 4.3 Kafka Consumer

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
    → CloudEvent.getId() → dltEventId
    → DltEventData fields → DltEvent fields
    ↓
IncomingDltEventManager.onDltEvent()
    ↓
DltEventPersistenceAdapter.save() → PostgreSQL
```

**PII-Schutz im Consumer:** JacksonException wird separat gecatcht — nur Exception-Klassenname und Parse-Location werden geloggt, nicht die Exception-Message (die JSON-Fragmente mit PII enthalten kann). Andere Exceptions werden ebenfalls nur mit Klassenname geloggt.

### 4.4 REST API

Alle Endpunkte erfordern JWT-Authentifizierung und Autorisierung über eine Whitelist (`authorization.users`).

| Methode | Pfad                             | Beschreibung                                         |
| ------- | -------------------------------- | ---------------------------------------------------- |
| GET     | `/api/events/overview`           | Übersicht aller DLT-Events (ohne Payload/StackTrace) |
| GET     | `/api/events/overview/{id}`      | Übersicht eines Events                               |
| GET     | `/api/events/details/{id}`       | Vollständige Details inkl. Payload und StackTrace    |
| POST    | `/api/events/re-processing/{id}` | Erneute Verarbeitung des Original-Events auslösen    |
| DELETE  | `/api/events/{id}`               | DLT-Event aus der Datenbank löschen                  |

### 4.5 Retry-Logik (Reprocessing)

```
Admin klickt "Retry" in der UI
    ↓
POST /api/events/re-processing/{dltEventId}
    ↓
DltEvent aus PostgreSQL laden
    ↓
Payload als Vorgang deserialisieren (decodeDltEventPayload)
    ↓
metadaten.rohdatenUuid extrahieren
    ↓ (null → DltEventAdminServiceException)
PapierantragEingangClient.resendPapierantrag(rohdatenUuid) via Feign
    ├── Erfolg → addAdminAction(TRIGGERED)
    └── FeignException → SafeExceptionLogger.sanitizeFeignException(e)
                       → addAdminAction(FAILED, safeMsg)
```

### 4.6 Security

**Autorisierung:** Whitelist von User-IDs in `authorization.users`. `WebSecurityConfig` definiert Custom SpEL-Ausdruck `isAuthorizedUser()`, der `uid`-Claim aus JWT prüft. Öffentliche Endpunkte (Swagger, Health, Metrics) via `permit-all`.

**PII-Schutz — SecurityControllerAdvice:**

- `ConstraintViolationException`: nur Property-Paths geloggt, keine Violation-Values. Error-ID für Log-Korrelation.
- `RuntimeException` und `HttpMessageNotReadableException`: noch nicht vollständig gehärtet — `ex.getMessage()` wird noch an den Client zurückgegeben. (**TODO: Error-ID Pattern implementieren**)
- `AccessDeniedException`: `ex.getMessage()` wird zurückgegeben (kein PII-Risiko, da Access-Denied-Nachrichten keine Payload-Daten enthalten).

### 4.7 Datenmodell (PostgreSQL)

```sql
dlt_event                              admin_action_history
┌──────────────────────────────┐       ┌──────────────────────────┐
│ dlt_event_id (PK)    TEXT    │  1──*─│ id (PK, IDENTITY) BIGINT │
│ original_event_id    TEXT    │       │ dlt_event_id (FK)  TEXT  │ ON DELETE CASCADE
│ service_name         TEXT    │       │ user_name          TEXT  │
│ add_to_dlt_timestamp TIMESTAMP│      │ performed_at    TIMESTAMP│
│ kafka_topic          TEXT    │       │ action_name        TEXT  │
│ kafka_partition      TEXT    │       │ action_details     TEXT  │
│ trace_id             TEXT    │       │ action_status VARCHAR(50)│
│ payload              TEXT    │       │ status_error       TEXT  │
│ payload_media_type   TEXT    │       └──────────────────────────┘
│ error                TEXT    │
│ stack_trace          TEXT    │       Indexes:
└──────────────────────────────┘       idx_admin_action_event_timestamp
                                         (dlt_event_id, performed_at)
Indexes:
idx_dlt_event_service_topic
  (service_name, kafka_topic)          ← Composite: filter by service, topic, or both
idx_dlt_event_add_to_dlt_timestamp
  (add_to_dlt_timestamp)
idx_dlt_event_trace_id
  (trace_id) WHERE trace_id IS NOT NULL  ← Partial: excludes NULLs
```

**Umbenannte Spalten** (PostgreSQL Reserved Keywords vermieden):

- `topic` → `kafka_topic`, `partition` → `kafka_partition` (Java field names bleiben `topic`/`partition`)
- `timestamp` → `performed_at`, `status` → `action_status` (Java field names bleiben `timestamp`/`status`)

**Admin Action History:** Statt des früheren `lastAdminAction` (embedded, überschreibend) wird jetzt eine separate Tabelle genutzt, die den vollständigen Verlauf aller Admin-Aktionen speichert. `DltEventEntity.getLastAdminAction()` gibt das erste Element der nach `timestamp DESC` sortierten Liste zurück.

### 4.8 JPA Entities

**DltEventEntity:**

- `@Entity` mit `@Table(name = "dlt_event")`
- `@OneToMany(mappedBy = "dltEvent", cascade = ALL, orphanRemoval = true)` für `adminActions`
- `@OrderBy("timestamp DESC")` für chronologische Sortierung (neueste zuerst)
- Builder Pattern (kein Lombok — JPA erfordert `protected` no-arg Constructor)
- `toString()` override: nur `dltEventId`, `serviceName`, `topic`, `addToDltTimestamp`

**AdminActionHistoryItemEntity:**

- `@Entity` mit `@Table(name = "admin_action_history")`
- `@ManyToOne(fetch = LAZY)` für `dltEvent`
- `@GeneratedValue(strategy = IDENTITY)` für auto-increment ID
- `toString()` override: nur `id`, `actionName`, `status`

### 4.9 Persistence Adapter

`DltEventPersistenceAdapter` (`@Service`):

- `findAll()` — `@Transactional(readOnly = true)`, nutzt `@EntityGraph(attributePaths = "adminActions")` für eager loading
- `findDltEventById()` — `@Transactional(readOnly = true)`
- `addAdminAction()` — `@Transactional`, findet Entity → `entity.addAdminAction()` → save
- `save()` — für Kafka Consumer incoming events
- `deleteByDltEventId()` — cascade löscht auch admin_action_history

**Repository Query (JPQL):**

```java
@EntityGraph(attributePaths = "adminActions")
@Query("""
    SELECT e FROM DltEventEntity e
    ORDER BY (
        SELECT MAX(a.timestamp) FROM AdminActionHistoryItemEntity a
        WHERE a.dltEvent.dltEventId = e.dltEventId
    ) DESC NULLS LAST
    """)
List<DltEventEntity> findAllOrderedByLastAdminActionDesc();
```

## 5. PII-Schutz (Personally Identifiable Information)

Das `Vorgang`-Payload enthält personenbezogene Daten (Name, Geburtsdatum, Adresse, Versicherungsnummern), die unter DSGVO/GDPR geschützt werden müssen.

### 5.1 Schutzebenen

| Ebene             | Klasse                                                                                           | Maßnahme                                                                                     |
| ----------------- | ------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------- |
| toString()        | `DltEvent`, `DltEventEntity`, `AdminActionHistoryItemEntity`, `DltEventData` (beide Subprojekte) | Nur safe Metadaten, nie payload/error/stackTrace                                             |
| Exception-Logging | `SafeExceptionLogger`                                                                            | `sanitizeFeignException()`: nur HTTP-Metadaten. `safeClassName()`: nur Exception-Klassenname |
| Kafka Consumer    | `DltEventConsumerAdapter`                                                                        | Separater catch für `JacksonException`: nur Klassenname + Parse-Location                     |
| HTTP Response     | `SecurityControllerAdvice`                                                                       | `ConstraintViolationException`: nur Property-Paths + Error-ID                                |

### 5.2 SafeExceptionLogger

```java
// FeignException — strips request/response body
SafeExceptionLogger.sanitizeFeignException(e)
→ "FeignException{status=503, method=POST, url=http://papierantrag-eingang/api/...}"

// Any exception — only class name, no message
SafeExceptionLogger.safeClassName(e)
→ "MismatchedInputException"
```

**Trigger-Stellen:**

- `DltEventAdminService.resendPapierantragImpl()` — FeignException bei fehlgeschlagenem Resend
- `DltEventAdminService.deleteDltEvent()` — Exception beim DB-Delete
- `DltEventConsumerAdapter.onDltEvent()` — JacksonException + generic Exception

### 5.3 Offene Punkte (TODO)

- `SecurityControllerAdvice.handleRuntimeException()` gibt noch `ex.getMessage()` an den Client zurück — sollte Error-ID Pattern verwenden
- `SecurityControllerAdvice.handleHttpMessageNotReadableException()` gibt noch `ex.getMessage()` an den Client zurück

## 6. Flyway Migration

```
db/migration/
└── V1__init_dlt_manager_db.sql       ← Erstellt beide Tabellen + Indexes

db/migration_role_schemata_admin/     ← Signal Iduna Pattern für Umgebungen mit DB-Rollen
├── beforeMigrate.sql                 ← call dbad.startchange_dlt_manager()
├── beforeEachMigrate.sql             ← set role ${role}
├── afterEachMigrate.sql              ← reset role
└── afterMigrate.sql                  ← call dbad.stopchange_dlt_manager()
```

## 7. Test-Architektur

```
Unit Tests (kein Spring Context)
  → DltEventPersistenceAdapterTest, EntityMapperTest, ApiModelMapperTest
  → SecurityControllerAdviceTest, PapierantragEingangAdapterTest
  → DltEventConsumerAdapterTest, SafeExceptionLoggerTest

Sliced Tests
  → DltManagerControllerTest (@WebMvcTest + spring-addons Security)

Integration Tests (Testcontainers PostgreSQL)
  → DltManagerApplicationTest (@SpringBootTest)
  → DltEventRepositoryIT (@SpringBootTest + @Transactional)
  → DltEventConsumerAdapterIT (@SpringBootTest + @EnableTestBinder)
  → DltEventAdminServiceTest (@SpringBootTest, WebEnvironment.NONE)
```

**Test-Infrastruktur:**

- `AbstractSingletonContainerTest`: Shared PostgreSQL Container (`@ServiceConnection`, `@Container`)
- `ContainerImageNames`: Zentralisierte Docker-Image-Namen (`postgres:16-alpine`)
- `SharedTestData`: Test-Vorgang mit PII-Daten und DLT_EVENT_1
- `TestUtil`: JSON-Vergleichs-Utilities (`assertThatJsonString`, `captureSingleArg`)

## 8. Konfiguration (application.yml)

| Bereich    | Konfiguration                                                                                      |
| ---------- | -------------------------------------------------------------------------------------------------- |
| PostgreSQL | `spring.datasource.*`, `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`   |
| Flyway     | `spring.flyway.locations`, `schemas`, `validate-migration-naming=true`, `baseline-on-migrate=true` |
| Kafka      | Spring Cloud Stream functional binding: `dltEventReceived-in-0` → `elpa-dlt` topic                 |
| Feign      | `papierantragEingangClient` → papierantrag-eingang service URL                                     |
| Security   | `com.c4-soft.springaddons.oidc.*`, `authorization.users` Whitelist                                 |
| Logging    | `de.signaliduna: DEBUG`, Logback JSON/Plain konfigurierbar                                         |

## 9. Zusammenfassung

Das DLT Manager System bietet eine robuste Fehlerbehandlungskette für Kafka-basierte Microservices:

1. **error-handler-lib** abstrahiert Fehlerklassifizierung und DLT-Einlieferung für alle elpa:4 Services
2. **backend** sammelt, speichert und verwaltet DLT-Events zentral mit PostgreSQL und REST API
3. **Admin Action History** erfasst den vollständigen Verlauf aller Admin-Aktionen (Retry, Delete) pro Event
4. **PII-Schutz** durch mehrschichtige Maßnahmen: toString()-Overrides, SafeExceptionLogger, sichere Exception-Handler

### Änderungen gegenüber v1 (MongoDB)

| Aspekt            | v1                                           | v2                                                                           |
| ----------------- | -------------------------------------------- | ---------------------------------------------------------------------------- |
| Datenbank         | MongoDB                                      | PostgreSQL + JPA + Flyway                                                    |
| Admin Actions     | `lastAdminAction` (embedded, überschreibend) | `admin_action_history` Tabelle (vollständiger Verlauf)                       |
| Query Pattern     | `MongoRepository` + `MongoOperations`        | `JpaRepository` + JPQL + `@EntityGraph`                                      |
| Entity Design     | Java Records                                 | JPA Entity Classes mit Builder Pattern                                       |
| PII-Schutz        | `DltEvent.toString()` override               | Umfassend: alle Records/Entities, SafeExceptionLogger, Consumer catch blocks |
| Reserved Keywords | `topic`, `partition` als Column-Namen        | `kafka_topic`, `kafka_partition`, `performed_at`, `action_status`            |
| Index-Strategie   | Keine (MongoDB default)                      | Composite, Partial, FK Indexes explizit definiert                            |
