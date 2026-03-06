package de.signaliduna.dltmanager.adapter.db.mapper;

import de.signaliduna.dltmanager.adapter.db.model.AdminActionHistoryItemEntity;
import de.signaliduna.dltmanager.adapter.db.model.DltEventEntity;
import de.signaliduna.dltmanager.core.model.AdminActionHistoryItem;
import de.signaliduna.dltmanager.core.model.DltEvent;
import jakarta.annotation.Nullable;

public class EntityMapper {
    
    public static DltEventEntity toDltEventEntity(DltEvent dltEvent) {
        return DltEventEntity.builder()
                .dltEventId(dltEvent.dltEventId())
                .originalEventId(dltEvent.originalEventId())
                .serviceName(dltEvent.serviceName())
                .addToDltTimestamp(dltEvent.addToDltTimestamp())
                .topic(dltEvent.topic())
                .partition(dltEvent.partition())
                .traceId(dltEvent.traceId())
                .payload(dltEvent.payload())
                .payloadMediaType(dltEvent.payloadMediaType())
                .error(dltEvent.error())
                .stackTrace(dltEvent.stackTrace())
                .build();
    }
    
    public static DltEvent fromDltEventEntity(DltEventEntity entity) {
        return new DltEvent(
                entity.getDltEventId(),
                entity.getOriginalEventId(),
                entity.getServiceName(),
                entity.getAddToDltTimestamp(),
                entity.getTopic(),
                entity.getPartition(),
                entity.getTraceId(),
                entity.getPayload(),
                entity.getPayloadMediaType(),
                entity.getError(),
                entity.getStackTrace(),
                fromAdminActionHistoryItemEntity(entity.getLastAdminAction())
        );
    }
    
    @Nullable
    public static AdminActionHistoryItem fromAdminActionHistoryItemEntity(@Nullable AdminActionHistoryItemEntity item) {
        if (item == null) {
            return null;
        }
        return new AdminActionHistoryItem(
                item.getUserName(),
                item.getTimestamp(),
                item.getActionName(),
                item.getActionDetails(),
                item.getStatus(),
                item.getStatusError()
        );
    }
    
    public static AdminActionHistoryItemEntity toAdminHistoryItemEntity(@Nullable AdminActionHistoryItem item) {
        if (item == null) {
            return null;
        }
        return AdminActionHistoryItemEntity.builder()
                .userName(item.userName())
                .timestamp(item.timestamp())
                .actionName(item.actionName())
                .actionDetails(item.actionDetails())
                .status(item.status())
                .statusError(item.statusError())
                .build();
    }
    
    private EntityMapper() {
    }
}
