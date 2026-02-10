package de.signaliduna.dltmanager.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.signaliduna.dltmanager.adapter.db.DltEventPersistenceAdapter;
import de.signaliduna.dltmanager.adapter.http.client.PapierantragEingangAdapter;
import de.signaliduna.dltmanager.core.model.DltEvent;
import de.signaliduna.elpa.sharedlib.model.Vorgang;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(
	classes = {DltEventAdminService.class, DltEventPersistenceAdapter.class, PapierantragEingangAdapter.class, ObjectMapper.class},
	webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestChannelBinderConfiguration.class)
class DltEventAdminServiceTest {
	@MockitoBean
	DltEventPersistenceAdapter dltEventPersistenceAdapter;
	@MockitoBean
	@SuppressWarnings("unused")
	PapierantragEingangAdapter papierantragEingangAdapter;
	@Autowired
	DltEventAdminService dltEventAdminService;

	@Test
	void deleteDltEvent_whenDltEventPersistenceAdapterThrows() {
		when(dltEventPersistenceAdapter.deleteByDltEventId(any())).thenThrow(new RuntimeException("Bad thing happened"));
		Assertions.assertThatThrownBy(() -> dltEventAdminService.deleteDltEvent("eventId", "userName"))
			.hasMessageContaining("Bad thing happened");
	}

	@Test
	void decodeDltEventPayload_whenDltEventPersistenceAdapterThrows() {
		@SuppressWarnings("BuilderMissingRequiredFields") final var event = DltEvent.builder().payload("bad json ;)").build();
		when(dltEventPersistenceAdapter.deleteByDltEventId(any())).thenThrow(new RuntimeException("Bad thing happened"));
		Assertions.assertThatThrownBy(() -> dltEventAdminService.decodeDltEventPayload(event, Vorgang.class))
			.hasMessageContaining("failed to decode payload of DltEvent");
	}
}
