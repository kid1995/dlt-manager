package de.signaliduna.dltmanager.adapter.http.api.model;


import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contains the parts of a DltEvent that are considered as useful to create an overview. The larger parts
 * of a DltEvent (e.g. payload and stackTrace) are omitted here but can be obtained if so by requesting {@code DltEventDetails}.
 */
public record DltEventOverviewItemDto(
	String dltEventId,
	String originalEventId,
	String serviceName,
	LocalDateTime addToDltTimestamp,
	String topic,
	String partition,
	@Nullable String traceId,
	String payloadMediaType,
	String error,
	@Nullable AdminActionHistoryItemDto lastAdminAction,
	List<DltEventActionDto> availableActions
) {
}
