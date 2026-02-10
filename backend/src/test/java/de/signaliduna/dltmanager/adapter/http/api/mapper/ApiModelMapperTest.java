package de.signaliduna.dltmanager.adapter.http.api.mapper;

import de.signaliduna.dltmanager.adapter.http.api.model.AdminActionHistoryItemDto;
import de.signaliduna.dltmanager.core.model.AdminActionHistoryItem;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApiModelMapperTest {
	@Nested
	class toAdminActionHistoryItemDto {
		@Test
		void domainObjectNull() {
			assertThat(ApiModelMapper.toAdminActionHistoryItemDto(null)).isNull();
		}

		@Test
		void domainObjectNotNull() {
			final var domainObject = new AdminActionHistoryItem("userName", LocalDateTime.now(), "actionName",
				"actionDetails", "status", "statusError");
			assertThat(ApiModelMapper.toAdminActionHistoryItemDto(domainObject)).isEqualTo(
				AdminActionHistoryItemDto.builder()
					.userName(domainObject.userName())
					.timestamp(domainObject.timestamp())
					.actionName(domainObject.actionName())
					.actionDetails(domainObject.actionDetails())
					.status(domainObject.status())
					.statusError(domainObject.statusError())
					.build()
			);
		}
	}
}
