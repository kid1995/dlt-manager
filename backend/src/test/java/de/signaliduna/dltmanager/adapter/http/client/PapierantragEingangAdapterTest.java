package de.signaliduna.dltmanager.adapter.http.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PapierantragEingangAdapterTest {

	@Mock
	PapierantragEingangClient papierantragEingangClient;

	@InjectMocks
	PapierantragEingangAdapter adapter;

	@Nested
	class resendPapierantrag {

		@Test
		void shouldWork() {
			final var antragsId = "antragsId";
			when(papierantragEingangClient.resendPapierantrag(antragsId)).thenReturn(ResponseEntity.status(HttpStatus.OK.value()).body("{}"));
			adapter.resendPapierantrag(antragsId);
			verify(papierantragEingangClient).resendPapierantrag(antragsId);
		}
	}

}
