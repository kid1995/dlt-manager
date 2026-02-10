package de.signaliduna.elpa.dltmanager.adapter.message.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.model.DltEventData;
import de.signaliduna.elpa.dltmanager.adapter.message.util.MessagingUtils;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

import static java.util.Objects.isNull;

public class DltTopicProducer {
	private static final Logger log = LoggerFactory.getLogger(DltTopicProducer.class);

	public record Props(
		String serviceName,
		String dltBindingName,
		String topicName,
		URI cloudEventSource,
		String cloudEventType
	) {
	}

	private final ObjectMapper objectMapper;
	private final Tracer tracer;
	private final StreamBridge streamBridge;
	private final DltTopicProducer.Props props;

	public DltTopicProducer(StreamBridge streamBridge, Tracer tracer, ObjectMapper objectMapper, DltTopicProducer.Props props) {
		this.streamBridge = streamBridge;
		this.tracer = tracer;
		this.objectMapper = objectMapper;
		this.props = props;
	}

	public String getTopicName() {
		return props.topicName;
	}

	public void createAndPublishDltEvent(MessagingException me, Message<?> failedMsg) {
		final CloudEvent dltEvent = createDltEvent(me, failedMsg);
		streamBridge.send(props.dltBindingName, dltEvent);
	}

	CloudEvent createDltEvent(MessagingException me, Message<?> failedMsg) {
		try {
			@Nullable final String originalEventId = MessagingUtils.idOf(me.getFailedMessage());
			final String receivedTopic = failedMsg.getHeaders().get(KafkaHeaders.RECEIVED_TOPIC, String.class);
			final String receivedPartition = failedMsg.getHeaders().get(KafkaHeaders.RECEIVED_PARTITION, String.class);
			final var errorMessage = (me.getCause() != null) ? me.getCause().getMessage() : me.getMessage();
			final var stackTraceString = ExceptionUtils.getStackTrace((me.getCause() != null) ? me.getCause() : me);
			final var timestamp = OffsetDateTime.now();

			String payload;
			if (failedMsg.getPayload() instanceof byte[] ba) {
				payload = new String(ba, StandardCharsets.UTF_8);
				log.debug("creating DltEvent from byte[] payload");
			} else {
				log.debug("creating DltEvent from payload of type {}", failedMsg.getPayload().getClass().getSimpleName());
				payload = objectMapper.writeValueAsString(failedMsg.getPayload());
			}

			final DltEventData dltEventData = DltEventData.builder()
				.originalEventId(originalEventId)
				.serviceName(props.serviceName)
				.addToDltTimestamp(timestamp.toLocalDateTime())
				.topic(receivedTopic)
				.partition(receivedPartition)
				.traceId(getTraceId())
				.payload(payload)
				.payloadMediaType(MediaType.APPLICATION_JSON_VALUE)
				.error(errorMessage)
				.stackTrace(stackTraceString)
				.build();

			return CloudEventBuilder.v1()
				.withSource(props.cloudEventSource)
				.withType(props.cloudEventType)
				.withId(UUID.randomUUID().toString())
				.withTime(timestamp)
				.withData(objectMapper.writeValueAsBytes(dltEventData))
				.build();
		} catch (JsonProcessingException e) {
			throw new SerializationFailedException("failed to serialize payload of failed message", e);
		}
	}

	@Nullable
	String getTraceId() {
		Span span = tracer.currentSpan();
		return (isNull(span)) ? null : span.context().traceId();
	}
}
