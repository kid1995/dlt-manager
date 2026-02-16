package de.signaliduna.dltmanager.adapter.db;

import de.signaliduna.dltmanager.adapter.db.model.AdminActionHistoryItemEntity;
import de.signaliduna.dltmanager.adapter.db.model.DltEventEntity;
import de.signaliduna.dltmanager.test.ContainerImageNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.shaded.com.google.common.net.MediaType;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataMongoTest(
	properties = {
		"jwt.enabled=false"
	}
)
class DltEventRepositoryIT{

	@Container
	@ServiceConnection
	static final MongoDBContainer mongoDBContainer = new MongoDBContainer(ContainerImageNames.MONGO.getImageName());

	private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2020, 1, 1, 0, 0);

	private static final DltEventEntity DEFAULT_DLT_EVENT = DltEventEntity.builder()
		.dltEventId(UUID.randomUUID().toString())
		.originalEventId("originalEventId")
		.serviceName("partnersync")
		.addToDltTimestamp(TIMESTAMP)
		.topic("topic")
		.partition("partition")
		.traceId("traceId")
		.payload("payload")
		.payloadMediaType(MediaType.ANY_APPLICATION_TYPE.type())
		.error("errorMsg")
		.stackTrace("stacktrace")
		.build();

	@Autowired
	DltEventRepository dltEventRepository;
	@Autowired
	MongoOperations mongoOperations;

	@AfterEach
	void afterEach() {
		mongoOperations.remove(new Query(), DltEventEntity.class);
	}

	@Nested
	class save {

		@Test
		void shouldSaveNewDltEvents() {
			assertThat(dltEventRepository.findAllByOrderByLastAdminActionDesc()).isEmpty();
			dltEventRepository.save(DEFAULT_DLT_EVENT);
			assertThat(dltEventRepository.findAllByOrderByLastAdminActionDesc()).containsExactly(DEFAULT_DLT_EVENT);
		}

		@Test
		void shouldNotInsertTheSameEntityTwice() {
			assertThat(dltEventRepository.findAllByOrderByLastAdminActionDesc()).isEmpty();
			dltEventRepository.save(DEFAULT_DLT_EVENT);
			dltEventRepository.save(DEFAULT_DLT_EVENT);
			assertThat(dltEventRepository.findAllByOrderByLastAdminActionDesc()).containsExactly(DEFAULT_DLT_EVENT);
		}

		@Test
		void shouldUpdateExistingEntitiesWithTheSameDltEventId() {
			assertThat(dltEventRepository.findAllByOrderByLastAdminActionDesc()).isEmpty();
			dltEventRepository.save(DEFAULT_DLT_EVENT);
			final var dltEventMod = DEFAULT_DLT_EVENT.toBuilder().error("updated error").build();
			dltEventRepository.save(dltEventMod);
			assertThat(dltEventRepository.findAllByOrderByLastAdminActionDesc()).containsExactly(dltEventMod);
		}
	}

	@Nested
	class updateLastAdminActionForDltEvent {
		final AdminActionHistoryItemEntity lastAdminAction = AdminActionHistoryItemEntity.builder()
			.userName("user1")
			.actionName("retry via Kafka")
			.actionDetails("action-details")
			.timestamp(TIMESTAMP)
			.status("ok")
			.build();

		@Test
		void shouldSaveNewLastAdminActionAndUpdateExistingOne() {
			final DltEventEntity entity = dltEventRepository.save(DEFAULT_DLT_EVENT);

			// save initial lastAdminAction value
			dltEventRepository.updateLastAdminActionForDltEvent(entity.dltEventId(), lastAdminAction);
			assertThat(dltEventRepository.findById(entity.dltEventId()).map(DltEventEntity::lastAdminAction)).contains(lastAdminAction);

			// update existing lastAdminAction value
			final var updatedAdminAction = lastAdminAction.toBuilder().timestamp(TIMESTAMP.plusMinutes(1)).statusError("action failed").build();
			dltEventRepository.updateLastAdminActionForDltEvent(entity.dltEventId(), updatedAdminAction);
			assertThat(dltEventRepository.findById(entity.dltEventId()).map(DltEventEntity::lastAdminAction)).contains(updatedAdminAction);
		}

		@Test
		void shouldReturnFalseWhenNotUpdated() {
			//given
			assertThat(dltEventRepository.findById(DEFAULT_DLT_EVENT.dltEventId())).isEmpty();

			//when/then
			assertThat(dltEventRepository.updateLastAdminActionForDltEvent(DEFAULT_DLT_EVENT.dltEventId(), lastAdminAction)).isFalse();
		}
	}

	@Nested
	class deleteByDltEventId {
		@Test
		void withNonExistingDltEventId() {
			assertThat(dltEventRepository.findAllByOrderByLastAdminActionDesc()).isEmpty();

			assertThat(dltEventRepository.deleteByDltEventId(DEFAULT_DLT_EVENT.dltEventId())).isZero();
		}

		@Test
		void withExistingDltEventId() {
			assertThat(dltEventRepository.findAllByOrderByLastAdminActionDesc()).isEmpty();
			dltEventRepository.save(DEFAULT_DLT_EVENT);

			assertThat(dltEventRepository.findAllByOrderByLastAdminActionDesc()).hasSize(1);

			assertThat(dltEventRepository.deleteByDltEventId(DEFAULT_DLT_EVENT.dltEventId())).isEqualTo( 1L);
			assertThat(dltEventRepository.findAllByOrderByLastAdminActionDesc()).isEmpty();
		}
	}

}
