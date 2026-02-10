package de.signaliduna.dltmanager.adapter.message.consumer.model;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

/**
 * Represents a failed Kafka event and its metadata. The payload is represented as {@link String} to avoid that
 * the service consuming the DLT message needs to know the model classes of the original event.
 */
public record DltEventData(
	String originalEventId,
	String serviceName,
	LocalDateTime addToDltTimestamp,
	@Nullable String topic,
	@Nullable String partition,
	@Nullable String traceId,
	String payload,
	String payloadMediaType,
	String error,
	@Nullable String stackTrace
) {
}
