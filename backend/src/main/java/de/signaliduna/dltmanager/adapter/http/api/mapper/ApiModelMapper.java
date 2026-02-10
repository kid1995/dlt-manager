package de.signaliduna.dltmanager.adapter.http.api.mapper;

import de.signaliduna.dltmanager.adapter.http.api.model.AdminActionHistoryItemDto;
import de.signaliduna.dltmanager.adapter.http.api.model.DltEventActionDto;
import de.signaliduna.dltmanager.adapter.http.api.model.DltEventFullItemDto;
import de.signaliduna.dltmanager.adapter.http.api.model.DltEventOverviewItemDto;
import de.signaliduna.dltmanager.core.model.AdminActionHistoryItem;
import de.signaliduna.dltmanager.core.model.DltEventAction;
import de.signaliduna.dltmanager.core.model.DltEventWithAdminActions;
import jakarta.annotation.Nullable;

public class ApiModelMapper {

	public static DltEventOverviewItemDto toDltEventOverviewItemDto(DltEventWithAdminActions data) {
		return new DltEventOverviewItemDto(
			data.dltEvent().dltEventId(),
			data.dltEvent().originalEventId(),
			data.dltEvent().serviceName(),
			data.dltEvent().addToDltTimestamp(),
			data.dltEvent().topic(),
			data.dltEvent().partition(),
			data.dltEvent().traceId(),
			data.dltEvent().payloadMediaType(),
			data.dltEvent().error(),
			toAdminActionHistoryItemDto(data.dltEvent().lastAdminAction()),
			data.availableActions().stream().map(ApiModelMapper::toDltEventActionDto).toList()
		);
	}

	public static DltEventFullItemDto toDltEventFullItemDto(DltEventWithAdminActions data) {
		return new DltEventFullItemDto(
			data.dltEvent().dltEventId(),
			data.dltEvent().originalEventId(),
			data.dltEvent().serviceName(),
			data.dltEvent().addToDltTimestamp(),
			data.dltEvent().topic(),
			data.dltEvent().partition(),
			data.dltEvent().traceId(),
			data.dltEvent().payload(),
			data.dltEvent().payloadMediaType(),
			data.dltEvent().error(),
			data.dltEvent().stackTrace(),
			toAdminActionHistoryItemDto(data.dltEvent().lastAdminAction()),
			data.availableActions().stream().map(ApiModelMapper::toDltEventActionDto).toList()
		);
	}

	public static DltEventActionDto toDltEventActionDto(DltEventAction action) {
		return new DltEventActionDto(action.name(), action.description());
	}

	public static AdminActionHistoryItemDto toAdminActionHistoryItemDto(@Nullable AdminActionHistoryItem item) {
		if (item == null) {
			return null;
		}
		return new AdminActionHistoryItemDto(
			item.userName(),
			item.timestamp(),
			item.actionName(),
			item.actionDetails(),
			item.status(),
			item.statusError()
		);
	}

	private ApiModelMapper() {
	}
}
