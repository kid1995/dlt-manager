package de.signaliduna.elpa.dltmanager.adapter.message.producer;

import tools.jackson.databind.ObjectMapper;
import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.model.DltEventData;
import de.signaliduna.elpa.sharedlib.model.Vorgang;
import io.cloudevents.CloudEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;

import static de.signaliduna.elpa.dltmanager.adapter.message.util.TestDataHelper.FeignExceptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DltTopicProducerTest {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final String DLT_BINDING = "dlt-binding";
	private static final URI CLOUD_EVENT_SOURCE = URI.create("/signal-iduna/elpa/someService");
	private static final String CLOUD_EVENT_TYPE = "de.signaliduna.elpa.someService.stream.onKafkaEvent-in.error";

	private static final DltTopicProducer.Props PROPS = new DltTopicProducer.Props(
		"serviceName", DLT_BINDING, "topicName", CLOUD_EVENT_SOURCE, CLOUD_EVENT_TYPE
	);

	private static final Vorgang VORGANG = Vorgang.builder().processId("processId").elpaId("elpaId").build();

	static {
	}

	@Mock
	StreamBridge streamBridge;
	@Mock
	Tracer tracer;

	private DltTopicProducer classUnderTest;

	@BeforeEach
	public void beforeEach() {
		if (classUnderTest == null) {
			classUnderTest = new DltTopicProducer(streamBridge, tracer, OBJECT_MAPPER, PROPS);
		}
	}

	@Nested
	class createDltEvent {
		@Test
		void happyPath_withVorgangPayload() throws IOException {
			// given
			final Message<Vorgang> failedMsg = new GenericMessage<>(VORGANG);
			final var messagingException = new MessagingException(failedMsg, FeignExceptions.badRequest());

			// when
			final CloudEvent dltEvent = classUnderTest.createDltEvent(messagingException, failedMsg);

			// then
			final DltEventData dltEventData = OBJECT_MAPPER.readValue(dltEvent.getData().toBytes(), DltEventData.class);
			assertThat(dltEventData.payload()).isEqualTo(OBJECT_MAPPER.writeValueAsString(VORGANG));
		}

		@Test
		void happyPath_withByteArrayPayload() throws IOException {
			// given
			final Message<byte[]> failedMsg = new GenericMessage<>(OBJECT_MAPPER.writeValueAsBytes(VORGANG));
			final var messagingException = new MessagingException(failedMsg, FeignExceptions.badRequest());

			// when
			final CloudEvent dltEvent = classUnderTest.createDltEvent(messagingException, failedMsg);

			// then
			final DltEventData dltEventData = OBJECT_MAPPER.readValue(dltEvent.getData().toBytes(), DltEventData.class);
			assertThat(dltEventData.payload()).isEqualTo(OBJECT_MAPPER.writeValueAsString(VORGANG));
		}

		@Test
		void withMessagingExceptionCauseIsNull() throws IOException {
			// given
			final var traceId = "myTraceId";
			mockTraceId(traceId);
			final Message<Vorgang> failedMsg = new GenericMessage<>(VORGANG);
			final var messagingException = new MessagingException(failedMsg, "messagingException message", null);

			// when
			final CloudEvent dltEvent = classUnderTest.createDltEvent(messagingException, failedMsg);

			// then
			final DltEventData dltEventData = OBJECT_MAPPER.readValue(dltEvent.getData().toBytes(), DltEventData.class);
			assertThat(dltEventData.traceId()).isEqualTo(traceId);
			assertThat(dltEventData.payload()).isEqualTo(OBJECT_MAPPER.writeValueAsString(VORGANG));
			assertThat(dltEventData.error()).isEqualTo(messagingException.getMessage());
		}

		@Test
		void withAJsonProcessingExceptionOccurring() {
			// given
			final var failedMsg = new GenericMessage<>(ZonedDateTime.now());
			final var messagingException = new MessagingException(failedMsg, "messagingException message", null);
			final var objectMapperFailingOnZonedDateTime = new ObjectMapper();
			final var publisher = new DltTopicProducer(streamBridge, tracer, objectMapperFailingOnZonedDateTime, PROPS);

			// when / then
			assertThatThrownBy(() ->
				publisher.createDltEvent(messagingException, failedMsg)).isInstanceOf(SerializationFailedException.class)
				.hasMessageContaining("failed to serialize payload of failed message")
				.cause().hasMessageContaining("`java.time.ZonedDateTime` not supported by default");
		}
	}

	@Nested
	class getTraceId {
		@Test
		void happyPath() {
			final var traceId = "myTraceId";
			mockTraceId(traceId);
			assertThat(classUnderTest.getTraceId()).isEqualTo(traceId);
		}

		@Test
		void shouldReturnNullWhenCurrentSpanIsNull() {
			when(tracer.currentSpan()).thenReturn(null);
			assertThat(classUnderTest.getTraceId()).isNull();
		}
	}

	private void mockTraceId(String traceId) {
		TraceContext traceContext = mock(TraceContext.class);
		when(traceContext.traceId()).thenReturn(traceId);
		Span span = mock(Span.class);
		when(span.context()).thenReturn(traceContext);
		when(tracer.currentSpan()).thenReturn(span);
	}
}
