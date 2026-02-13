package de.signaliduna.dltmanager.adapter.db;

import de.signaliduna.dltmanager.adapter.db.mapper.EntityMapper;
import de.signaliduna.dltmanager.core.model.AdminAction;
import de.signaliduna.dltmanager.core.model.AdminActionHistoryItem;
import de.signaliduna.dltmanager.core.model.DltEventAction;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static de.signaliduna.dltmanager.test.SharedTestData.DLT_EVENT_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DltEventPersistenceAdapterTest {
	private static final String DLT_EVENT1_ID = DLT_EVENT_1.dltEventId();

	@Mock
	DltEventRepository dltEventRepository;
	@InjectMocks
	DltEventPersistenceAdapter dltEventPersistenceAdapter;


	@Nested
	class streamAll {
		@Test
		void shouldWork() {
			when(dltEventRepository.findAllByOrderByLastAdminActionDesc()).thenReturn(Stream.of(EntityMapper.toDltEventEntity(DLT_EVENT_1)));
			assertThat(dltEventPersistenceAdapter.streamAll()).containsExactly(DLT_EVENT_1);
		}
	}

	@Nested
	class findDltEventById {
		@Test
		void shouldReturnDataForKnownDltEventId() {
			when(dltEventRepository.findById(DLT_EVENT1_ID)).thenReturn(Optional.of(EntityMapper.toDltEventEntity(DLT_EVENT_1)));
			assertThat(dltEventPersistenceAdapter.findDltEventById(DLT_EVENT1_ID)).contains(DLT_EVENT_1);
		}
	}

	@Nested
	class updateLastAdminActionForDltEvent {
		@Test
		void shouldReturnTrueOnSuccess() {
			when(dltEventRepository.updateLastAdminActionForDltEvent(eq(DLT_EVENT1_ID), any())).thenReturn(true);
			final var item = AdminActionHistoryItem.builder()
				.actionName(AdminAction.RESEND_TO_PAPIERANTRAG_EINGANG.name())
				.timestamp(LocalDateTime.now())
				.userName("userName").status(DltEventAction.Status.FAILED.name()).build();
			assertThat(dltEventPersistenceAdapter.updateLastAdminActionForDltEvent(DLT_EVENT1_ID, item)).isTrue();
		}
	}


	@Nested
	class deleteByDltEventId {
		@Test
		void shouldReturnTrueForExistingDltEventId() {
			when(dltEventRepository.deleteByDltEventId(DLT_EVENT1_ID)).thenReturn(1L);
			assertThat(dltEventPersistenceAdapter.deleteByDltEventId(DLT_EVENT1_ID)).isTrue();
		}

		@Test
		void shouldReturnFalseForExistingDltEventId() {
			when(dltEventRepository.deleteByDltEventId(DLT_EVENT1_ID)).thenReturn(0L);
			assertThat(dltEventPersistenceAdapter.deleteByDltEventId(DLT_EVENT1_ID)).isFalse();
		}
	}

}
