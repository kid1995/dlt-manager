package de.signaliduna.elpa.dltmanager.adapter.message.producer;

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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.Objects;

import static de.signaliduna.elpa.dltmanager.adapter.message.util.TestDataHelper.FeignExceptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DltTopicProducerTest {
	private static final JsonMapper JSON_MAPPER = new JsonMapper();
	private static final String DLT_BINDING = "dlt-binding";
	private static final URI CLOUD_EVENT_SOURCE = URI.create("/signal-iduna/elpa/someService");
	private static final String CLOUD_EVENT_TYPE = "de.signaliduna.elpa.someService.stream.onKafkaEvent-in.error";

	private static final DltTopicProducer.Props PROPS = new DltTopicProducer.Props(
		"serviceName", DLT_BINDING, "topicName", CLOUD_EVENT_SOURCE, CLOUD_EVENT_TYPE
	);

	private static final Vorgang VORGANG = Vorgang.builder().processId("processId").elpaId("elpaId").build();

	@Mock
	StreamBridge streamBridge;
	@Mock
	Tracer tracer;

	private DltTopicProducer classUnderTest;

	@BeforeEach
	void beforeEach() {
		if (classUnderTest == null) {
			classUnderTest = new DltTopicProducer(streamBridge, tracer, JSON_MAPPER, PROPS);
		}
	}

	@Nested
	class createDltEvent {
		@Test
		void happyPath_withVorgangPayload() {
			// given
			final Message<Vorgang> failedMsg = new GenericMessage<>(VORGANG);
			final var messagingException = new MessagingException(failedMsg, FeignExceptions.badRequest());

			// when
			final CloudEvent dltEvent = classUnderTest.createDltEvent(messagingException, failedMsg);

			// then
			final DltEventData dltEventData = JSON_MAPPER.readValue(Objects.requireNonNull(dltEvent.getData()).toBytes(), DltEventData.class);
			assertThat(dltEventData.payload()).isEqualTo(JSON_MAPPER.writeValueAsString(VORGANG));
		}

		@Test
		void happyPath_withByteArrayPayload() {
			// given
			final Message<byte[]> failedMsg = new GenericMessage<>(JSON_MAPPER.writeValueAsBytes(VORGANG));
			final var messagingException = new MessagingException(failedMsg, FeignExceptions.badRequest());

			// when
			final CloudEvent dltEvent = classUnderTest.createDltEvent(messagingException, failedMsg);

			// then
			final DltEventData dltEventData = JSON_MAPPER.readValue(Objects.requireNonNull(dltEvent.getData()).toBytes(), DltEventData.class);
			assertThat(dltEventData.payload()).isEqualTo(JSON_MAPPER.writeValueAsString(VORGANG));
		}

		@Test
		void withMessagingExceptionCauseIsNull() throws JacksonException {
			// given
			final var traceId = "myTraceId";
			createMockTraceId();
			final Message<Vorgang> failedMsg = new GenericMessage<>(VORGANG);
			final var messagingException = new MessagingException(failedMsg, "messagingException message", null);

			// when
			final CloudEvent dltEvent = classUnderTest.createDltEvent(messagingException, failedMsg);

			// then
			final DltEventData dltEventData = JSON_MAPPER.readValue(Objects.requireNonNull(dltEvent.getData()).toBytes(), DltEventData.class);
			assertThat(dltEventData.traceId()).isEqualTo(traceId);
			assertThat(dltEventData.payload()).isEqualTo(JSON_MAPPER.writeValueAsString(VORGANG));
			assertThat(dltEventData.error()).isEqualTo(messagingException.getMessage());
		}

	}

	@Nested
	class getTraceId {
		@Test
		void happyPath() {
			final var traceId = "myTraceId";
			createMockTraceId();
			assertThat(classUnderTest.getTraceId()).isEqualTo(traceId);
		}

		@Test
		void shouldReturnNullWhenCurrentSpanIsNull() {
			when(tracer.currentSpan()).thenReturn(null);
			assertThat(classUnderTest.getTraceId()).isNull();
		}
	}

	private void createMockTraceId() {
		TraceContext traceContext = mock(TraceContext.class);
		when(traceContext.traceId()).thenReturn("myTraceId");
		Span span = mock(Span.class);
		when(span.context()).thenReturn(traceContext);
		when(tracer.currentSpan()).thenReturn(span);
	}
}
