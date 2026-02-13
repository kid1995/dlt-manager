package de.signaliduna.elpa.dltmanager.adapter.message.errorhandler;

import de.signaliduna.elpa.sharedlib.model.AllgemeineDaten;
import de.signaliduna.elpa.sharedlib.model.Antrag;
import de.signaliduna.elpa.sharedlib.model.Vorgang;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.GenericMessage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class VorgangProcessIdExtractorTest {
	private static final Vorgang TEST_VORGANG = Vorgang.builder()
		.processId("processId")
		.elpaId("elpaId")
		.antrag(
			Antrag.builder().allgemeineDaten(
					AllgemeineDaten.builder().antragstellungsdatum(LocalDate.of(2024, 11, 20)).build())
				.build()).build();

	private static final JsonMapper JSON_MAPPER = new JsonMapper();

	@Nested
	class extractId {
		@Test
		void testMessageWithVorgangPayload() {
			final var extractor = new VorgangProcessIdExtractor(JSON_MAPPER);
			assertThat(extractor.extractId(new GenericMessage<>(TEST_VORGANG))).isEqualTo("processId");
		}

		@Test
		void testMessageWithByteArrayPayload() {
			final var extractor = new VorgangProcessIdExtractor(JSON_MAPPER);
			final var message = new GenericMessage<>(createTestPayloadAsJsonString().getBytes(StandardCharsets.UTF_8));
			assertThat(extractor.extractId(message)).isEqualTo("processId");
		}

		@Test
		void testMessageWithByteArrayPayloadNull() {
			final var extractor = new VorgangProcessIdExtractor(JSON_MAPPER);
			final var message = new GenericMessage<>("bad json".getBytes(StandardCharsets.UTF_8));
			assertThatThrownBy(() -> extractor.extractId(message))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("failed to read Vorgang");
		}

		@Test
		void testMessageWithStringPayload() {
			final var extractor = new VorgangProcessIdExtractor(JSON_MAPPER);
			final var message = new GenericMessage<>(createTestPayloadAsJsonString());
			assertThat(extractor.extractId(message)).isEqualTo("processId");
		}
	}

	@Test
	void extractVorgangProcessIdShouldThrowIllegalArgumentExceptionForObjectWithoutProcessId() {
		final var extractor = new VorgangProcessIdExtractor(JsonMapper.builder().build());
		final var payload = """
			{"processEiDIEEH": "processId"}""".getBytes(StandardCharsets.UTF_8);
		assertThatThrownBy(() -> extractor.extractVorgangProcessId(payload))
			.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("failed to extract Vorgang.processId: field not found");
	}

	private static String createTestPayloadAsJsonString() {
		try {
			return JSON_MAPPER.writeValueAsString(VorgangProcessIdExtractorTest.TEST_VORGANG);
		} catch (JacksonException e) {
			throw new RuntimeException(e);
		}
	}
}
