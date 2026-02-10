package de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.model;

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

	public Builder toBuilder() {
		return new Builder(this);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private String originalEventId;
		private String serviceName;
		private LocalDateTime addToDltTimestamp;
		private String topic;
		private String partition;
		private String traceId;
		private String payload;
		private String payloadMediaType;
		private String error;
		private String stackTrace;

		private Builder() {
		}

		private Builder(DltEventData source) {
			this.originalEventId = source.originalEventId;
			this.serviceName = source.serviceName;
			this.addToDltTimestamp = source.addToDltTimestamp;
			this.topic = source.topic;
			this.partition = source.partition;
			this.traceId = source.traceId;
			this.payload = source.payload;
			this.payloadMediaType = source.payloadMediaType;
			this.error = source.error;
			this.stackTrace = source.stackTrace;
		}

		public Builder originalEventId(String originalEventId) {
			this.originalEventId = originalEventId;
			return this;
		}

		public Builder serviceName(String serviceName) {
			this.serviceName = serviceName;
			return this;
		}

		public Builder addToDltTimestamp(LocalDateTime addToDltTimestamp) {
			this.addToDltTimestamp = addToDltTimestamp;
			return this;
		}

		public Builder topic(String topic) {
			this.topic = topic;
			return this;
		}

		public Builder partition(String partition) {
			this.partition = partition;
			return this;
		}

		public Builder traceId(String traceId) {
			this.traceId = traceId;
			return this;
		}

		public Builder payload(String payload) {
			this.payload = payload;
			return this;
		}

		public Builder payloadMediaType(String payloadMediaType) {
			this.payloadMediaType = payloadMediaType;
			return this;
		}

		public Builder error(String error) {
			this.error = error;
			return this;
		}

		public Builder stackTrace(String stackTrace) {
			this.stackTrace = stackTrace;
			return this;
		}

		public DltEventData build() {
			return new DltEventData(this.originalEventId, this.serviceName, this.addToDltTimestamp, this.topic,
				this.partition, this.traceId, this.payload, this.payloadMediaType, this.error, this.stackTrace);
		}
	}
}
