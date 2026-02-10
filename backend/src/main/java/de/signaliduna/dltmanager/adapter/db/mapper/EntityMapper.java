package de.signaliduna.dltmanager.adapter.db.mapper;

import de.signaliduna.dltmanager.adapter.db.model.AdminActionHistoryItemEntity;
import de.signaliduna.dltmanager.adapter.db.model.DltEventEntity;
import de.signaliduna.dltmanager.core.model.AdminActionHistoryItem;
import de.signaliduna.dltmanager.core.model.DltEvent;
import jakarta.annotation.Nullable;

public class EntityMapper {

	public static DltEventEntity toDltEventEntity(DltEvent dltEvent) {
		return new DltEventEntity(
			dltEvent.dltEventId(),
			dltEvent.originalEventId(),
			dltEvent.serviceName(),
			dltEvent.addToDltTimestamp(),
			dltEvent.topic(),
			dltEvent.partition(),
			dltEvent.traceId(),
			dltEvent.payload(),
			dltEvent.payloadMediaType(),
			dltEvent.error(),
			dltEvent.stackTrace(),
			toAdminHistoryItemEntity(dltEvent.lastAdminAction())
		);
	}

	public static DltEvent fromDltEventEntity(DltEventEntity entity) {
		return new DltEvent(
			entity.dltEventId(),
			entity.originalEventId(),
			entity.serviceName(),
			entity.addToDltTimestamp(),
			entity.topic(),
			entity.partition(),
			entity.traceId(),
			entity.payload(),
			entity.payloadMediaType(),
			entity.error(),
			entity.stackTrace(),
			fromAdminActionHistoryItemEntity(entity.lastAdminAction())
		);
	}

	public static AdminActionHistoryItem fromAdminActionHistoryItemEntity(@Nullable AdminActionHistoryItemEntity item) {
		if(item == null) {
			return null;
		}
		return new AdminActionHistoryItem(
			item.userName(),
			item.timestamp(),
			item.actionName(),
			item.actionDetails(),
			item.status(),
			item.statusError()
		);
	}

	public static AdminActionHistoryItemEntity toAdminHistoryItemEntity(@Nullable AdminActionHistoryItem item) {
		if(item == null) {
			return null;
		}
		return new AdminActionHistoryItemEntity(
			item.userName(),
			item.timestamp(),
			item.actionName(),
			item.actionDetails(),
			item.status(),
			item.statusError()
		);
	}

	private EntityMapper() {
	}
}
