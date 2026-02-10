package de.signaliduna.elpa.dltmanager.adapter.message.errorhandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.signaliduna.elpa.sharedlib.model.AllgemeineDaten;
import de.signaliduna.elpa.sharedlib.model.Antrag;
import de.signaliduna.elpa.sharedlib.model.Vorgang;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.GenericMessage;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class VorgangProcessIdExtractorTest {
	private static final Vorgang VORGANG = Vorgang.builder()
		.processId("processId")
		.elpaId("elpaId")
		.antrag(
			Antrag.builder().allgemeineDaten(
					AllgemeineDaten.builder().antragstellungsdatum(LocalDate.of(2024, 11, 20)).build())
				.build()).build();

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	static {
		OBJECT_MAPPER.registerModule(new JavaTimeModule());
	}

	@Nested
	class extractId {
		@Test
		void testMessageWithVorgangPayload() {
			final var extractor = new VorgangProcessIdExtractor(OBJECT_MAPPER);
			assertThat(extractor.extractId(new GenericMessage<>(VORGANG))).isEqualTo("processId");
		}

		@Test
		void testMessageWithByteArrayPayload() {
			final var extractor = new VorgangProcessIdExtractor(OBJECT_MAPPER);
			final var message = new GenericMessage<>(asJsonString(VORGANG).getBytes(StandardCharsets.UTF_8));
			assertThat(extractor.extractId(message)).isEqualTo("processId");
		}

		@Test
		void testMessageWithByteArrayPayloadNull() {
			final var extractor = new VorgangProcessIdExtractor(OBJECT_MAPPER);
			final var message = new GenericMessage<>("bad json".getBytes(StandardCharsets.UTF_8));
			assertThatThrownBy(() -> extractor.extractId(message))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("failed to read Vorgang");
		}

		@Test
		void testMessageWithStringPayload() {
			final var extractor = new VorgangProcessIdExtractor(OBJECT_MAPPER);
			final var message = new GenericMessage<>(asJsonString(VORGANG));
			assertThat(extractor.extractId(message)).isEqualTo("processId");
		}
	}

	@Test
	void asJsonBytesShouldThrowIllegalArgumentExceptionWhenPayloadIsNull() {
		final var extractor = new VorgangProcessIdExtractor(new ObjectMapper());
		assertThatThrownBy(() -> extractor.asJsonBytes(VORGANG))
			.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("failed write value as JSON bytes")
			.cause().hasMessageContaining("LocalDate` not supported by default");
	}

	@Test
	void extractVorgangProcessIdShouldThrowIllegalArgumentExceptionForObjectWithoutProcessId() {
		final var extractor = new VorgangProcessIdExtractor(new ObjectMapper());
		final var payload = """
			{"processEiDIEEH": "processId"}""".getBytes(StandardCharsets.UTF_8);
		assertThatThrownBy(() -> extractor.extractVorgangProcessId(payload))
			.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("failed to extract Vorgang.processId: field not found");
	}

	private static String asJsonString(final Object obj) {
		try {
			return OBJECT_MAPPER.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
