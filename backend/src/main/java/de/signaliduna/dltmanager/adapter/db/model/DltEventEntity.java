package de.signaliduna.dltmanager.adapter.db.model;

import jakarta.annotation.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("DltEvent")
public record DltEventEntity(
	@Id String dltEventId,
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

	@Nullable AdminActionHistoryItemEntity lastAdminAction
) {

	public static final String FIELD_id = "dltEventId";
	public static final String FIELD_addToDltTimestamp = "addToDltTimestamp";
	public static final String FIELD_lastAdminAction = "lastAdminAction";

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
		private AdminActionHistoryItemEntity lastAdminAction;

		private Builder() {
		}

		private Builder(DltEventEntity source) {
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

		public Builder lastAdminAction(AdminActionHistoryItemEntity lastAdminAction) {
			this.lastAdminAction = lastAdminAction;
			return this;
		}

		public DltEventEntity build() {
			return new DltEventEntity(this.dltEventId, this.originalEventId, this.serviceName,
				this.addToDltTimestamp, this.topic, this.partition, this.traceId, this.payload,
				this.payloadMediaType, this.error, this.stackTrace, this.lastAdminAction);
		}
	}
}
