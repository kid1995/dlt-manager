# OpenRewrite Extended Recipe – Coverage Matrix

## ✅ Automated by Extended Recipe

| Change | Recipe | Example from hint-service |
|---|---|---|
| `@MockBean` → `@MockitoBean` | Community | `HintAdapterIT`, `HintApiIT` |
| `@SpyBean` → `@MockitoSpyBean` | Community | — |
| `starter-web` → `starter-webmvc` | Community | `hint-service/build.gradle` |
| `starter-aop` → `starter-aspectj` | Community | `hint-service/build.gradle` |
| `starter-oauth2-resource-server` → `starter-security-oauth2-resource-server` | Community | `hint-service/build.gradle` |
| Modular test starters added | Community | `*-webmvc-test`, `*-data-jpa-test`, etc. |
| `spring-boot-starter-test` split | Community | Replaced by specific starters |
| Spring Boot properties migration | Community | `application.yml` property keys |
| SpringDoc 2.x → 3.0 | Community | `springdoc-openapi-starter-webmvc-ui:3.0.1` |
| Testcontainers `junit-jupiter` → `testcontainers-junit-jupiter` | Community | `hint-service/build.gradle` |
| Jackson `com.fasterxml.jackson.*` → `tools.jackson.*` | **Jackson recipe** | `HintClientIT`, `HintApiIT` |
| Jackson `ObjectMapper` → `JsonMapper.builder()` | **Jackson recipe** | `TestConfig.jsonMapper()` |
| Jackson method renames (`writeObject`→`writePOJO`) | **Jackson recipe** | — |
| Jackson exception simplification | **Jackson recipe** | unchecked in 3.x |
| Testcontainers `postgresql` → `testcontainers-postgresql` | **Extended** | `hint-service/build.gradle` |
| `org.testcontainers.containers.PostgreSQLContainer` → `org.testcontainers.postgresql.PostgreSQLContainer` | **Extended** | `HintRepositoryIT`, `AbstractSingletonContainerTest` |
| Testcontainers `mongodb` → `testcontainers-mongodb` | **Extended** | dlt-manager |
| `@DataJpaTest` package relocation | **Extended** | `HintRepositoryIT`, `HintSpecificationsIT` |
| `@WebMvcTest` package relocation | **Extended** | `HintApiIT`, `WebSecurityConfigTest` |
| Spring Cloud → 2025.1.0 | **Extended** | `build.gradle ext` |
| spring-addons → 9.x | **Extended** | `spring-addons-starter-oidc:9.1.0` |
| springwolf-kafka → 2.x | **Extended** | `springwolf-kafka:2.0.0` |
| logstash-logback-encoder → 9.x | **Extended** | `logstash-logback-encoder:9.0` |
| WireMock Spring Boot → 4.x | **Extended** | `wiremock-spring-boot:4.0.9` |
| MapStruct → latest 1.6.x | **Extended** | `mapstructVersion:1.6.3` |

## ⚠️ Manual Changes Still Required

These changes cannot be automated by OpenRewrite and must be done manually:

### 1. spring-addons 9.x API Changes
The recipe bumps the version, but API changes require manual refactoring:

```java
// WebSecurityConfig.java – already migrated in hint-service
// Key: SpringAddonsMethodSecurityExpressionHandler + ExpressionRoot API
// may differ between 7.x and 9.x
```

### 2. `logstash-logback-encoder` Jackson Exclude
When using Jackson 3.x, the exclude group changes:
```groovy
// Old (Jackson 2.x)
implementation('net.logstash.logback:logstash-logback-encoder:9.0') {
    exclude group: 'com.fasterxml.jackson.core', module: 'jackson-databind'
}
// New (Jackson 3.x) – verify if exclude is still needed
// Spring Boot 4 manages Jackson 3.x, so the exclude may need updating
// to tools.jackson.core:jackson-databind or may no longer be necessary
```

### 3. Spring Cloud Stream Kafka Binder Properties
Check if any `spring.cloud.stream.kafka.binder.*` properties changed in 2025.1.0.

### 4. `@EnableWireMock` Annotation
Already present in hint-service (`HintClientIT`). If migrating from an older WireMock version, ensure:
- Import: `org.wiremock.spring.EnableWireMock`
- Property: `wiremock.server.port` injection works with Boot 4

### 5. Gradle Wrapper Version
hint-service uses Gradle 8.10. Ensure compatibility with OpenRewrite plugin. Recommended: Gradle 8.10+.

### 6. `HintService2.java` Cleanup
This file appears to be a draft/alternative with metrics. It has compilation issues (missing imports, wrong class name in constructor). Should be cleaned up or removed.

### 7. `application.yml` Property Review
- `management.observations.spring.security.filterchains.enabled` → deprecated in Boot 4, hint-service uses `MeterFilter` bean instead (see `MetricsConfig.java`)
- `spring.jpa.hibernate.naming.implicit-strategy` → path may change (handled by community recipe)

## 📋 Recipe Dependencies

```groovy
dependencies {
    rewrite("org.openrewrite.recipe:rewrite-spring:6.23.1")       // Spring Boot 4 community
    rewrite("org.openrewrite.recipe:rewrite-jackson:1.15.0")      // Jackson 2→3
    rewrite("org.openrewrite.recipe:rewrite-testing-frameworks:3.8.0") // Testcontainers 2.x
}
```
