package de.signaliduna.dltmanager.adapter.db;

import de.signaliduna.dltmanager.adapter.db.model.AdminActionHistoryItemEntity;
import de.signaliduna.dltmanager.adapter.db.model.DltEventEntity;
import de.signaliduna.dltmanager.test.AbstractSingletonContainerTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"jwt.enabled=false", "com.c4-soft.springaddons.oidc.resourceserver.enabled=false"})
class DltEventRepositoryIT extends AbstractSingletonContainerTest {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2020, 1, 1, 0, 0);

    @Autowired
    DltEventRepository dltEventRepository;

    @Autowired
    EntityManager entityManager;

    @AfterEach
    void afterEach() {
        dltEventRepository.deleteAll();
    }

    private DltEventEntity createEvent(String dltEventId) {
        return DltEventEntity.builder()
            .dltEventId(dltEventId)
            .originalEventId("originalEventId")
            .serviceName("partnersync")
            .addToDltTimestamp(TIMESTAMP)
            .topic("topic")
            .partition("partition")
            .traceId("traceId")
            .payload("payload")
            .payloadMediaType("application/json")
            .error("errorMsg")
            .stackTrace("stacktrace")
            .build();
    }

    @Nested
    class save {

        @Test
        void shouldSaveNewDltEvents() {
            assertThat(dltEventRepository.findAllOrderedByLastAdminActionDesc()).isEmpty();
            dltEventRepository.save(createEvent(UUID.randomUUID().toString()));
            assertThat(dltEventRepository.findAllOrderedByLastAdminActionDesc()).hasSize(1);
        }

        @Test
        void shouldNotInsertTheSameEntityTwice() {
            String id = UUID.randomUUID().toString();
            dltEventRepository.save(createEvent(id));
            dltEventRepository.save(createEvent(id));
            assertThat(dltEventRepository.findAllOrderedByLastAdminActionDesc()).hasSize(1);
        }

        @Test
        @Transactional
        void shouldUpdateExistingEntitiesWithTheSameDltEventId() {
            String id = UUID.randomUUID().toString();
            dltEventRepository.save(createEvent(id));
            DltEventEntity found = dltEventRepository.findById(id).orElseThrow();
            found.setError("updated error");
            dltEventRepository.save(found);
            assertThat(dltEventRepository.findById(id))
                .isPresent()
                .get()
                .extracting(DltEventEntity::getError)
                .isEqualTo("updated error");
        }
    }

    @Nested
    class addAdminAction {

        @Test
        @Transactional
        void shouldAddAdminAction() {
            String id = UUID.randomUUID().toString();
            DltEventEntity event = dltEventRepository.save(createEvent(id));
            event.addAdminAction(AdminActionHistoryItemEntity.builder()
                .userName("user1").actionName("RETRY").actionDetails("details")
                .timestamp(TIMESTAMP).status("SUCCESS").build());
            dltEventRepository.save(event);
            entityManager.flush();
            entityManager.clear();
            DltEventEntity found = dltEventRepository.findById(id).orElseThrow();
            assertThat(found.getAdminActions()).hasSize(1);
            assertThat(found.getLastAdminAction()).isNotNull();
            assertThat(found.getLastAdminAction().getActionName()).isEqualTo("RETRY");
        }

        @Test
        @Transactional
        void shouldReturnNullLastAdminActionWhenNoActions() {
            String id = UUID.randomUUID().toString();
            dltEventRepository.save(createEvent(id));
            DltEventEntity found = dltEventRepository.findById(id).orElseThrow();
            assertThat(found.getLastAdminAction()).isNull();
        }
    }

    @Nested
    class deleteByDltEventId {

        @Test
        void withNonExistingDltEventId() {
            assertThat(dltEventRepository.deleteByDltEventId("non-existing")).isZero();
        }

        @Test
        @Transactional
        void withExistingDltEventId() {
            String id = UUID.randomUUID().toString();
            dltEventRepository.save(createEvent(id));
            assertThat(dltEventRepository.findAllOrderedByLastAdminActionDesc()).hasSize(1);
            assertThat(dltEventRepository.deleteByDltEventId(id)).isEqualTo(1L);
            assertThat(dltEventRepository.findAllOrderedByLastAdminActionDesc()).isEmpty();
        }

        @Test
        @Transactional
        void shouldCascadeDeleteAdminActions() {
            String id = UUID.randomUUID().toString();
            DltEventEntity event = dltEventRepository.save(createEvent(id));
            event.addAdminAction(AdminActionHistoryItemEntity.builder()
                .userName("user1").actionName("RETRY").timestamp(TIMESTAMP).status("SUCCESS").build());
            dltEventRepository.save(event);
            entityManager.flush();
            assertThat(dltEventRepository.deleteByDltEventId(id)).isEqualTo(1L);
            assertThat(dltEventRepository.findById(id)).isEmpty();
        }
    }

    @Nested
    class findAllOrderedByLastAdminActionDesc {

        @Test
        void shouldReturnEmptyWhenNoEvents() {
            assertThat(dltEventRepository.findAllOrderedByLastAdminActionDesc()).isEmpty();
        }

        @Test
        @Transactional
        void shouldOrderByLastAdminActionTimestampDesc() {
            String id1 = UUID.randomUUID().toString();
            DltEventEntity event1 = dltEventRepository.save(createEvent(id1));
            event1.addAdminAction(AdminActionHistoryItemEntity.builder()
                .userName("u").actionName("A").timestamp(TIMESTAMP.plusMinutes(10)).status("OK").build());
            dltEventRepository.save(event1);

            String id2 = UUID.randomUUID().toString();
            DltEventEntity event2 = dltEventRepository.save(createEvent(id2));
            event2.addAdminAction(AdminActionHistoryItemEntity.builder()
                .userName("u").actionName("A").timestamp(TIMESTAMP.plusMinutes(5)).status("OK").build());
            dltEventRepository.save(event2);

            String id3 = UUID.randomUUID().toString();
            dltEventRepository.save(createEvent(id3));

            entityManager.flush();
            entityManager.clear();

            var result = dltEventRepository.findAllOrderedByLastAdminActionDesc();
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getDltEventId()).isEqualTo(id1);
            assertThat(result.get(1).getDltEventId()).isEqualTo(id2);
            assertThat(result.get(2).getDltEventId()).isEqualTo(id3);
        }

        @Test
        @Transactional
        void shouldEagerlyLoadAdminActions() {
            String id = UUID.randomUUID().toString();
            DltEventEntity event = dltEventRepository.save(createEvent(id));
            event.addAdminAction(AdminActionHistoryItemEntity.builder()
                .userName("user").actionName("RETRY").timestamp(TIMESTAMP).status("OK").build());
            dltEventRepository.save(event);
            entityManager.flush();
            entityManager.clear();
            var result = dltEventRepository.findAllOrderedByLastAdminActionDesc();
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getAdminActions()).hasSize(1);
        }
    }
}
