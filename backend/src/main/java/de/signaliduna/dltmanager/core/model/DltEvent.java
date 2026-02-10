package de.signaliduna.dltmanager.core.model;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record DltEvent(
	String dltEventId,
	String originalEventId,
	String serviceName,
	LocalDateTime addToDltTimestamp,
	String topic,
	String partition,
	@Nullable String traceId,
	String payload,
	String payloadMediaType,
	@Nullable String error,
	@Nullable String stackTrace,
	@Nullable AdminActionHistoryItem lastAdminAction
) {
	@Override
	public String toString() {
		return getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private String dltEventId;
		private String originalEventId;
		private String serviceName;
		private LocalDateTime addToDltTimestamp;
		private String topic;
		private String partition;
		@Nullable
		private String traceId;
		private String payload;
		private String payloadMediaType;
		@Nullable
		private String error;
		@Nullable
		private String stackTrace;
		@Nullable
		private AdminActionHistoryItem lastAdminAction;

		private Builder() {
		}

		private Builder(DltEvent source) {
			this.dltEventId = source.dltEventId;
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
			this.lastAdminAction = source.lastAdminAction;
		}

		public Builder dltEventId(String dltEventId) {
			this.dltEventId = dltEventId;
			return this;
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

		public Builder lastAdminAction(AdminActionHistoryItem lastAdminAction) {
			this.lastAdminAction = lastAdminAction;
			return this;
		}

		public DltEvent build() {
			return new DltEvent(this.dltEventId, this.originalEventId, this.serviceName,
				this.addToDltTimestamp, this.topic, this.partition, this.traceId, this.payload,
				this.payloadMediaType, this.error, this.stackTrace, this.lastAdminAction);
		}
	}
}
