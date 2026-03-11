package de.signaliduna.dltmanager.adapter.http.api.model;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Contains the full data of a DltEvent.
 */
public record DltEventFullItemDto(
	UUID dltEventId,
	String originalEventId,
	String serviceName,
	LocalDateTime addToDltTimestamp,
	String topic,
	String partition,
	@Nullable String traceId,
	String payload,
	String payloadMediaType,
	String error,
	@Nullable String stackTrace,
	@Nullable AdminActionHistoryItemDto lastAdminAction,
	List<DltEventActionDto> availableActions
) {
}
