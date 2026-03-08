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
import java.util.List;
import java.util.Optional;

import static de.signaliduna.dltmanager.test.SharedTestData.DLT_EVENT_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DltEventPersistenceAdapterTest {

    private static final String DLT_EVENT1_ID = DLT_EVENT_1.dltEventId();

    @Mock
    DltEventRepository dltEventRepository;
    @InjectMocks
    DltEventPersistenceAdapter dltEventPersistenceAdapter;

    @Nested
    class findAll {
        @Test
        void shouldWork() {
            when(dltEventRepository.findAllOrderedByLastAdminActionDesc())
                .thenReturn(List.of(EntityMapper.toDltEventEntity(DLT_EVENT_1)));
            assertThat(dltEventPersistenceAdapter.findAll()).containsExactly(DLT_EVENT_1);
        }
    }

    @Nested
    class findDltEventById {
        @Test
        void shouldReturnDataForKnownDltEventId() {
            when(dltEventRepository.findById(DLT_EVENT1_ID))
                .thenReturn(Optional.of(EntityMapper.toDltEventEntity(DLT_EVENT_1)));
            assertThat(dltEventPersistenceAdapter.findDltEventById(DLT_EVENT1_ID)).contains(DLT_EVENT_1);
        }
    }

    @Nested
    class addAdminAction {
        @Test
        void shouldReturnTrueWhenEventFound() {
            final var entity = EntityMapper.toDltEventEntity(DLT_EVENT_1);
            when(dltEventRepository.findById(DLT_EVENT1_ID)).thenReturn(Optional.of(entity));
            when(dltEventRepository.save(any())).thenReturn(entity);
            final var item = AdminActionHistoryItem.builder()
                .actionName(AdminAction.RESEND_TO_PAPIERANTRAG_EINGANG.name())
                .timestamp(LocalDateTime.now())
                .userName("userName")
                .status(DltEventAction.Status.TRIGGERED.name())
                .build();
            assertThat(dltEventPersistenceAdapter.addAdminAction(DLT_EVENT1_ID, item)).isTrue();
        }

        @Test
        void shouldReturnFalseWhenEventNotFound() {
            when(dltEventRepository.findById(DLT_EVENT1_ID)).thenReturn(Optional.empty());
            final var item = AdminActionHistoryItem.builder()
                .actionName(AdminAction.RESEND_TO_PAPIERANTRAG_EINGANG.name())
                .timestamp(LocalDateTime.now())
                .userName("userName")
                .status(DltEventAction.Status.FAILED.name())
                .build();
            assertThat(dltEventPersistenceAdapter.addAdminAction(DLT_EVENT1_ID, item)).isFalse();
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
