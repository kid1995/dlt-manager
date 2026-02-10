package de.signaliduna.dltmanager.adapter.db.mapper;

import de.signaliduna.dltmanager.adapter.db.model.AdminActionHistoryItemEntity;
import de.signaliduna.dltmanager.core.model.AdminActionHistoryItem;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMapperTest {

	@Nested
	class fromAdminActionHistoryItemEntity {
		@Test
		void entityNull() {
			assertThat(EntityMapper.fromAdminActionHistoryItemEntity(null)).isNull();
		}

		@Test
		void entityNotNull() {
			final var entity = new AdminActionHistoryItemEntity("userName", LocalDateTime.now(), "actionName",
				"actionDetails", "status", "statusError");
			assertThat(EntityMapper.fromAdminActionHistoryItemEntity(entity)).isEqualTo(
				AdminActionHistoryItem.builder()
					.userName(entity.userName())
					.timestamp(entity.timestamp())
					.actionName(entity.actionName())
					.actionDetails(entity.actionDetails())
					.status(entity.status())
					.statusError(entity.statusError())
					.build()
			);
		}
	}

	@Nested
	class toAdminHistoryItemEntity {
		@Test
		void entityNull() {
			assertThat(EntityMapper.toAdminHistoryItemEntity(null)).isNull();
		}

		@Test
		void entityNotNull() {
			final var entity = new AdminActionHistoryItem("userName", LocalDateTime.now(), "actionName",
				"actionDetails", "status", "statusError");
			assertThat(EntityMapper.toAdminHistoryItemEntity(entity)).isEqualTo(
				AdminActionHistoryItemEntity.builder()
					.userName(entity.userName())
					.timestamp(entity.timestamp())
					.actionName(entity.actionName())
					.actionDetails(entity.actionDetails())
					.status(entity.status())
					.statusError(entity.statusError())
					.build()
			);
		}
	}
}
