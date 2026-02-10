package de.signaliduna.dltmanager.adapter.http.api.model;

import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

public record AdminActionHistoryItemDto(
	String userName,
	LocalDateTime timestamp,
	String actionName,
	String actionDetails,
	String status,
	@Nullable String statusError
) {
	public boolean isSuccess() {
		return StringUtils.isEmpty(statusError);
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private String userName;
		private LocalDateTime timestamp;
		private String actionName;
		private String actionDetails;
		private String status;
		@Nullable private String statusError;

		private Builder() {
		}

		private Builder(AdminActionHistoryItemDto source) {
			this.userName = source.userName;
			this.timestamp = source.timestamp;
			this.actionName = source.actionName;
			this.actionDetails = source.actionDetails;
			this.status = source.status;
			this.statusError = source.statusError;
		}

		public Builder userName(String userName) {
			this.userName = userName;
			return this;
		}

		public Builder timestamp(LocalDateTime timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public Builder actionName(String actionName) {
			this.actionName = actionName;
			return this;
		}

		public Builder actionDetails(String actionDetails) {
			this.actionDetails = actionDetails;
			return this;
		}

		public Builder status(String status) {
			this.status = status;
			return this;
		}

		public Builder statusError(String statusError) {
			this.statusError = statusError;
			return this;
		}

		public AdminActionHistoryItemDto build() {
			return new AdminActionHistoryItemDto(this.userName, this.timestamp, this.actionName,
				this.actionDetails, this.status, this.statusError);
		}
	}
}
