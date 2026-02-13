package de.signaliduna.elpa.dltmanager.adapter.message.errorhandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker.http.FeignExceptionRecoverabilityChecker;
import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker.http.RecoverableHttpErrorCodes;
import de.signaliduna.elpa.dltmanager.adapter.message.producer.DltTopicProducer;
import de.signaliduna.elpa.dltmanager.adapter.message.producer.RetryTopicProducer;
import de.signaliduna.elpa.sharedlib.model.Vorgang;
import io.cloudevents.CloudEvent;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static de.signaliduna.elpa.dltmanager.adapter.message.util.TestDataHelper.FeignExceptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorHandlerTest {
	public static final String RETRY_BINDING = "retry-binding";
	public static final String DLT_BINDING = "dlt-binding";
	private static final URI CLOUD_EVENT_SOURCE = URI.create("/signal-iduna/elpa/someService");
	private static final String CLOUD_EVENT_TYPE = "de.signaliduna.elpa.someService.stream.onKafkaEvent-in.error";
	private static final Vorgang VORGANG = Vorgang.builder().processId("processId").elpaId("elpaId").build();

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	static {
		OBJECT_MAPPER.registerModule(new JavaTimeModule());
	}

	@Mock
	StreamBridge streamBridge;
	@Mock
	Tracer tracer;
	ErrorHandler classUnderTest;

	@BeforeEach
	void beforeEach() {
		tracer = mock(Tracer.class);
		if (classUnderTest == null) {
			final var processIdExtractor = new VorgangProcessIdExtractor(OBJECT_MAPPER);
			final var props = new DltTopicProducer.Props("serviceName", DLT_BINDING, "dltTopic", CLOUD_EVENT_SOURCE, CLOUD_EVENT_TYPE);
			classUnderTest = Mockito.spy(new ErrorHandler(
				new RetryTopicProducer(streamBridge, RETRY_BINDING, "retryTopic"),
				new DltTopicProducer(streamBridge, tracer, OBJECT_MAPPER, props), processIdExtractor, new FeignExceptionRecoverabilityChecker(new RecoverableHttpErrorCodes())
			));
		}
	}

	@Nested
	class accept {

		@Test
		void shouldTerminateNormallyWhenPayloadIsOfWrongType() {
			final var errorMessage = new ErrorMessage(new RuntimeException("unexpected exception type"));

			classUnderTest.accept(errorMessage);

			verify(classUnderTest).logUnexpectedMessage(eq("accept - expected errorMessage.getPayload() to be a MessagingException but was java.lang.RuntimeException"), any());
			verify(streamBridge, never()).send(eq(DLT_BINDING), any());
			verify(streamBridge, never()).send(eq(RETRY_BINDING), any());
		}

		@Test
		void shouldTerminateNormallyWhenFailedMessageIsNull() {
			final var messagingException = new MessagingException((GenericMessage<Vorgang>) null, FeignExceptions.unavailable());
			final var errorMessage = new ErrorMessage(messagingException);

			classUnderTest.accept(errorMessage);

			verify(classUnderTest).logUnexpectedMessage(eq("messagingException.getFailedMessage() message is null"), any());
			verify(streamBridge, never()).send(eq(DLT_BINDING), any());
			verify(streamBridge, never()).send(eq(RETRY_BINDING), any());
		}

		@Test
		void shouldPublishVorgangToRetryTopicOnRecoverableException() {
			final var messagingException = new MessagingException(new GenericMessage<>(VORGANG), FeignExceptions.unavailable());
			final var errorMessage = new ErrorMessage(messagingException);

			classUnderTest.accept(errorMessage);

			verify(classUnderTest, never()).logUnexpectedMessage(any(), any());
			verify(streamBridge, never()).send(eq(DLT_BINDING), any());
			verify(streamBridge).send(RETRY_BINDING, VORGANG);
		}

		@Test
		void shouldPublishCloudEventToDltTopicOnNonRecoverableException() {
			final var messagingException = new MessagingException(new GenericMessage<>(VORGANG), FeignExceptions.badRequest());
			final var errorMessage = new ErrorMessage(messagingException);

			classUnderTest.accept(errorMessage);

			verify(classUnderTest, never()).logUnexpectedMessage(any(), any());
			verify(streamBridge).send(eq(DLT_BINDING), any(CloudEvent.class));
			verify(streamBridge, never()).send(eq(RETRY_BINDING), any());
		}
	}

	@Nested
	class getIdInfo {
		@Test
		void withValidJsonPayload() {
			final var message = new GenericMessage<>(asJsonString(Vorgang.builder().processId("processId").build()).getBytes(StandardCharsets.UTF_8));
			assertThat(classUnderTest.getIdInfo(message)).isEqualTo("processId=processId");
		}

		@Test
		void withInvalidJsonPayload() {
			final var message = new GenericMessage<>("bad json".getBytes(StandardCharsets.UTF_8));
			assertThat(classUnderTest.getIdInfo(message)).isEqualTo("<failed to extract id>");
		}
	}

	@Nested
	class getCauseOrThrowable {
		@Test
		void noCause() {
			final var throwableNoCause = mock(Throwable.class);
			assertThat(ErrorHandler.getCauseOrThrowable(throwableNoCause)).isEqualTo(throwableNoCause);
		}

		@Test
		void withCause() {
			final var throwableWithCause = mock(Throwable.class);
			when(throwableWithCause.getCause()).thenReturn(new IllegalArgumentException("bad argument"));
			assertThat(ErrorHandler.getCauseOrThrowable(throwableWithCause)).isSameAs(throwableWithCause.getCause());
		}
	}

	private static String asJsonString(final Object obj) {
		try {
			return OBJECT_MAPPER.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}

