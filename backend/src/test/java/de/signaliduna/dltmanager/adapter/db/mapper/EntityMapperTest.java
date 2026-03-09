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
            final LocalDateTime now = LocalDateTime.now();
            final var entity = AdminActionHistoryItemEntity.builder()
                .userName("userName")
                .timestamp(now)
                .actionName("actionName")
                .actionDetails("actionDetails")
                .status("status")
                .statusError("statusError")
                .build();
            assertThat(EntityMapper.fromAdminActionHistoryItemEntity(entity)).isEqualTo(
                AdminActionHistoryItem.builder()
                    .userName(entity.getUserName())
                    .timestamp(entity.getTimestamp())
                    .actionName(entity.getActionName())
                    .actionDetails(entity.getActionDetails())
                    .status(entity.getStatus())
                    .statusError(entity.getStatusError())
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
            final LocalDateTime now = LocalDateTime.now();
            final var item = new AdminActionHistoryItem("userName", now, "actionName",
                "actionDetails", "status", "statusError");
            assertThat(EntityMapper.toAdminHistoryItemEntity(item))
                .usingRecursiveComparison()
                .ignoringFields("id", "dltEvent")
                .isEqualTo(
                    AdminActionHistoryItemEntity.builder()
                        .userName(item.userName())
                        .timestamp(item.timestamp())
                        .actionName(item.actionName())
                        .actionDetails(item.actionDetails())
                        .status(item.status())
                        .statusError(item.statusError())
                        .build()
                );
        }
    }
}
