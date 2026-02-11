# CloudEvents JSON-Jackson entfernen – Jackson 3 / Spring Boot 4 Migration

## Zusammenfassung

| Aspekt | Vorher (Jackson 2) | Nachher (Jackson 3) |
|---|---|---|
| Mapper-Klasse | `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.json.JsonMapper` |
| Auto-configured Bean | `ObjectMapper` | `JsonMapper` (Spring Boot 4 auto-config) |
| Exceptions | Checked (`JsonProcessingException`) | Unchecked (`JacksonException`) |
| CloudEvents Deserialization | `PojoCloudEventDataMapper.from(objectMapper, ...)` | `jsonMapper.readValue(event.getData().toBytes(), ...)` |
| Annotations Package | `com.fasterxml.jackson.annotation` | `com.fasterxml.jackson.annotation` (unverändert!) |

> **Warum `JsonMapper` statt `ObjectMapper`?**
> - `JsonMapper` ist **immutable** nach `build()` → thread-safe ohne Synchronisation
> - Spring Boot 4 konfiguriert automatisch einen `JsonMapper`-Bean
> - `Jackson2ObjectMapperBuilder` ist deprecated → `JsonMapperBuilderCustomizer` verwenden
> - `JsonMapper extends ObjectMapper` → überall kompatibel wo `ObjectMapper` erwartet wird

---

## 1. `build.gradle` – Dependency ändern

```groovy
// ❌ ENTFERNEN
implementation("io.cloudevents:cloudevents-json-jackson:${cloudeventsVersion}")

// ✅ BEHALTEN (diese Module haben KEINE Jackson-Abhängigkeit)
implementation("io.cloudevents:cloudevents-core:${cloudeventsVersion}")
implementation("io.cloudevents:cloudevents-spring:${cloudeventsVersion}")
implementation("io.cloudevents:cloudevents-kafka:${cloudeventsVersion}")  // falls verwendet
```

> **Hinweis:** `cloudevents-core`, `cloudevents-spring` und `cloudevents-kafka` hängen **nicht** von Jackson ab.
> Nur `cloudevents-json-jackson` bringt die Jackson-2-Abhängigkeit mit.

---

## 2. `DltEventConsumerAdapter.java` – Hauptänderung

### Vorher (Jackson 2 + cloudevents-json-jackson):
```java
package de.signaliduna.dltmanager.adapter.message.consumer;

import de.signaliduna.dltmanager.adapter.message.consumer.mapper.EventDataMapper;
import de.signaliduna.dltmanager.adapter.message.consumer.model.DltEventData;
import de.signaliduna.dltmanager.core.model.DltEvent;
import de.signaliduna.dltmanager.core.service.IncomingDltEventManager;
import io.cloudevents.CloudEvent;
import io.cloudevents.jackson.PojoCloudEventDataMapper;       // ← aus cloudevents-json-jackson
import com.fasterxml.jackson.databind.ObjectMapper;            // ← Jackson 2
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Consumer;

import static io.cloudevents.core.CloudEventUtils.mapData;     // ← braucht cloudevents-json-jackson

@Component
public class DltEventConsumerAdapter {
    private static final Logger log = LoggerFactory.getLogger(DltEventConsumerAdapter.class);
    private final IncomingDltEventManager dltEventManager;
    private final ObjectMapper objectMapper;

    public DltEventConsumerAdapter(IncomingDltEventManager dltEventManager,
                                    ObjectMapper objectMapper) {
        this.dltEventManager = dltEventManager;
        this.objectMapper = objectMapper;
    }

    @Bean
    public Consumer<CloudEvent> dltEventReceived() {
        return this::onDltEvent;
    }

    private void onDltEvent(CloudEvent event) {
        try {
            final DltEventData eventData = Objects.requireNonNull(
                mapData(event, PojoCloudEventDataMapper.from(objectMapper, DltEventData.class))
            ).getValue();
            final DltEvent dltEvent = EventDataMapper.toDomainObject(event, eventData);
            log.atInfo().log("Received DltEvent from {} (dltEventId={}, originalEventId={}, traceId={})",
                eventData.serviceName(), dltEvent.dltEventId(), eventData.originalEventId(), dltEvent.traceId());
            dltEventManager.onDltEvent(dltEvent);
        } catch (Exception e) {
            log.atError().log("Failed to process DltEvent (event.source={}, event.type={}, event.id={})",
                event.getSource(), event.getType(), event.getId(), e);
        }
    }
}
```

### Nachher (Jackson 3 `JsonMapper`, kein cloudevents-json-jackson):
```java
package de.signaliduna.dltmanager.adapter.message.consumer;

import de.signaliduna.dltmanager.adapter.message.consumer.mapper.EventDataMapper;
import de.signaliduna.dltmanager.adapter.message.consumer.model.DltEventData;
import de.signaliduna.dltmanager.core.model.DltEvent;
import de.signaliduna.dltmanager.core.service.IncomingDltEventManager;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import tools.jackson.databind.json.JsonMapper;                // ← Jackson 3 JsonMapper
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class DltEventConsumerAdapter {
    private static final Logger log = LoggerFactory.getLogger(DltEventConsumerAdapter.class);
    private final IncomingDltEventManager dltEventManager;
    private final JsonMapper jsonMapper;                       // ← Spring Boot 4 auto-configured

    public DltEventConsumerAdapter(IncomingDltEventManager dltEventManager,
                                    JsonMapper jsonMapper) {
        this.dltEventManager = dltEventManager;
        this.jsonMapper = jsonMapper;
    }

    @Bean
    public Consumer<CloudEvent> dltEventReceived() {
        return this::onDltEvent;
    }

    private void onDltEvent(CloudEvent event) {
        try {
            final CloudEventData data = event.getData();
            if (data == null) {
                log.atWarn().log("Received CloudEvent without data (id={})", event.getId());
                return;
            }

            // Direkt mit Jackson 3 JsonMapper deserialisieren – kein PojoCloudEventDataMapper nötig
            final DltEventData eventData = jsonMapper.readValue(data.toBytes(), DltEventData.class);

            final DltEvent dltEvent = EventDataMapper.toDomainObject(event, eventData);
            log.atInfo().log("Received DltEvent from {} (dltEventId={}, originalEventId={}, traceId={})",
                eventData.serviceName(), dltEvent.dltEventId(), eventData.originalEventId(), dltEvent.traceId());
            dltEventManager.onDltEvent(dltEvent);
        } catch (Exception e) {
            log.atError().log("Failed to process DltEvent (event.source={}, event.type={}, event.id={})",
                event.getSource(), event.getType(), event.getId(), e);
        }
    }
}
```

### Was sich geändert hat:
| # | Änderung | Grund |
|---|---|---|
| 1 | `ObjectMapper` → `JsonMapper` | Jackson 3 Best Practice, immutable, auto-configured |
| 2 | `PojoCloudEventDataMapper.from(...)` entfernt | War aus `cloudevents-json-jackson` (Jackson 2) |
| 3 | `mapData(...)` static import entfernt | Nicht mehr benötigt |
| 4 | `event.getData().toBytes()` + `jsonMapper.readValue()` | Direkte Deserialisierung, kein Wrapper |
| 5 | Null-Check für `event.getData()` | Defensiv – `PojoCloudEventDataMapper` machte das intern |
| 6 | Kein `Objects.requireNonNull().getValue()` | Einfacher und klarer Kontrollfluss |

---

## 3. `DltEventAdminService.java` – `objectMapper.readValue()` für Vorgang

Falls `DltEventAdminService` ebenfalls `ObjectMapper` injiziert bekommt (für Payload-Decoding bei `resendPapierantrag()`):

### Vorher:
```java
import com.fasterxml.jackson.databind.ObjectMapper;   // Jackson 2

public class DltEventAdminService {
    private final ObjectMapper objectMapper;
    // ...

    public void resendPapierantrag(String dltEventId, String user) {
        // ...
        Vorgang vorgang = objectMapper.readValue(dltEvent.payload(), Vorgang.class);
        // ...
    }
}
```

### Nachher:
```java
import tools.jackson.databind.json.JsonMapper;        // Jackson 3

public class DltEventAdminService {
    private final JsonMapper jsonMapper;
    // ...

    public void resendPapierantrag(String dltEventId, String user) {
        // ...
        Vorgang vorgang = jsonMapper.readValue(dltEvent.payload(), Vorgang.class);
        // ...
    }
}
```

> **Hinweis:** `readValue()` wirft in Jackson 3 `JacksonException` (unchecked) statt `JsonProcessingException` (checked).
> Bestehende `try-catch` Blöcke mit `Exception` funktionieren weiterhin.
> Aber explizite `catch (JsonProcessingException e)` muss zu `catch (JacksonException e)` geändert werden:
> ```java
> // Jackson 2
> import com.fasterxml.jackson.core.JsonProcessingException;
> 
> // Jackson 3
> import tools.jackson.core.JacksonException;
> ```

---

## 4. `error-handler-lib` (DltTopicProducer) – Kein Handlungsbedarf im Backend

`DltTopicProducer` im `error-handler-lib` verwendet:
```java
CloudEventBuilder.v1()
    .withData(objectMapper.writeValueAsBytes(dltEventData))
    .build();
```

Das nutzt `cloudevents-core` (nicht `cloudevents-json-jackson`), weil `.withData(byte[])` nur raw bytes erwartet.

**Aber:** `error-handler-lib` ist eine **separate Library**, die von anderen ELPA-Services benutzt wird.
Diese Services laufen möglicherweise noch auf Spring Boot 3 mit Jackson 2.
→ Die Library **sollte vorerst bei Jackson 2 bleiben**, bis alle Consumer-Services ebenfalls migriert sind.
→ Das betrifft **nicht** den `dlt-manager` Backend – dort wird nur `cloudevents-core` benötigt.

---

## 5. Test-Anpassungen

### `DltEventConsumerAdapterTest.java`

Falls der Test `PojoCloudEventDataMapper` oder einen Jackson-2-ObjectMapper verwendet:

```java
// ❌ Vorher
import com.fasterxml.jackson.databind.ObjectMapper;

@Test
void shouldProcessDltEvent() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    // ...
}

// ✅ Nachher
import tools.jackson.databind.json.JsonMapper;

@Test
void shouldProcessDltEvent() {
    JsonMapper jsonMapper = JsonMapper.builder().build();
    // Jackson 3: JavaTimeModule ist standardmäßig registriert!
    // ...
}
```

### CloudEvent Test-Daten erstellen (ohne cloudevents-json-jackson):

```java
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

// Test-CloudEvent mit raw bytes (kein JsonCloudEventData nötig)
CloudEvent testEvent = CloudEventBuilder.v1()
    .withId("test-id")
    .withType("de.signaliduna.elpa.partnersync.error")
    .withSource(URI.create("/signal-iduna/elpa/partnersync"))
    .withDataContentType("application/json")
    .withData(jsonMapper.writeValueAsBytes(testDltEventData))  // Jackson 3 Bytes
    .build();
```

---

## 6. `config/rewrite.yml` – OpenRewrite Exclude-Regel

Damit OpenRewrite bei zukünftigen Runs `cloudevents-json-jackson` nicht erneut hinzufügt:

```yaml
# CloudEvents SDK verwendet intern Jackson 2 – nicht migrieren
- org.openrewrite.java.dependencies.RemoveDependency:
    groupId: io.cloudevents
    artifactId: cloudevents-json-jackson
```

---

## 7. Checkliste

- [ ] `cloudevents-json-jackson` aus `build.gradle` entfernen
- [ ] `cloudevents-core` und `cloudevents-spring` Dependencies behalten
- [ ] `DltEventConsumerAdapter`: `ObjectMapper` → `JsonMapper`, `PojoCloudEventDataMapper` → direkt `readValue()`
- [ ] `DltEventAdminService`: `ObjectMapper` → `JsonMapper` (für Vorgang-Decoding)
- [ ] Alle `import com.fasterxml.jackson.databind.*` → `import tools.jackson.databind.*` (außer Annotations!)
- [ ] `import com.fasterxml.jackson.core.JsonProcessingException` → `import tools.jackson.core.JacksonException`
- [ ] Tests: `new ObjectMapper()` → `JsonMapper.builder().build()`
- [ ] Tests: `JavaTimeModule` manuell registrieren entfällt (Jackson 3 Standard)
- [ ] `error-handler-lib` bleibt vorerst bei Jackson 2 (separate Library)
- [ ] `rewrite.yml` Exclude-Regel hinzufügen
- [ ] Kompilieren & alle Tests grün

---

## Hinweis zu `@JsonProperty`, `@JsonIgnore` etc.

Jackson-Annotations behalten ihr altes Package:
```java
// Bleibt unverändert in Jackson 3!
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
```

Das ist Absicht für Rückwärtskompatibilität zwischen Jackson 2 und 3.
