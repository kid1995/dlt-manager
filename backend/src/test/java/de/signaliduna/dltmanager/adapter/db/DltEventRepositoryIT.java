package de.signaliduna.dltmanager.adapter.db;

import de.signaliduna.dltmanager.adapter.db.model.AdminActionHistoryItemEntity;
import de.signaliduna.dltmanager.adapter.db.model.DltEventEntity;
import de.signaliduna.dltmanager.test.ContainerImageNames;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"jwt.enabled=false"})
class DltEventRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer(
        DockerImageName.parse(ContainerImageNames.POSTGRES.getImageName())
            .asCompatibleSubstituteFor(PostgreSQLContainer.IMAGE)
    );

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2020, 1, 1, 0, 0);

    @Autowired
    DltEventRepository dltEventRepository;

    @Autowired
    EntityManager entityManager;

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
        void withExistingDltEventId() {
            String id = UUID.randomUUID().toString();
            dltEventRepository.save(createEvent(id));
            assertThat(dltEventRepository.findAllOrderedByLastAdminActionDesc()).hasSize(1);
            assertThat(dltEventRepository.deleteByDltEventId(id)).isEqualTo(1L);
            assertThat(dltEventRepository.findAllOrderedByLastAdminActionDesc()).isEmpty();
        }

        @Test
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
