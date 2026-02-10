package de.signaliduna.dltmanager.adapter.message.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.signaliduna.dltmanager.adapter.message.consumer.mapper.EventDataMapper;
import de.signaliduna.dltmanager.adapter.message.consumer.model.DltEventData;
import de.signaliduna.dltmanager.core.model.DltEvent;
import de.signaliduna.dltmanager.core.service.IncomingDltEventManager;
import io.cloudevents.CloudEvent;
import io.cloudevents.jackson.PojoCloudEventDataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Consumer;

import static io.cloudevents.core.CloudEventUtils.mapData;

@Component
public class DltEventConsumerAdapter {
	private static final Logger log = LoggerFactory.getLogger(DltEventConsumerAdapter.class);
	private final IncomingDltEventManager dltEventManager;
	private final ObjectMapper objectMapper;

	public DltEventConsumerAdapter(IncomingDltEventManager dltEventManager, ObjectMapper objectMapper) {
		this.dltEventManager = dltEventManager;
		this.objectMapper = objectMapper;
	}

	@Bean
	public Consumer<CloudEvent> dltEventReceived() {
		return this::onDltEvent;
	}

	private void onDltEvent(CloudEvent event) {
		try {
			final DltEventData eventData = Objects.requireNonNull(mapData(event, PojoCloudEventDataMapper.from(objectMapper, DltEventData.class))).getValue();
			final DltEvent dltEvent = EventDataMapper.toDomainObject(event, eventData);
			log.atInfo().log("Received DltEvent from {} (dltEventId={}, originalEventId={}, traceId={})",
				eventData.serviceName(), dltEvent.dltEventId(), eventData.originalEventId(), dltEvent.traceId());
			dltEventManager.onDltEvent(dltEvent);
		} catch (Exception e) {
			log.atError().log("Failed to process DltEvent (event.source={}, event.type={}, event.id={})",
				event.getSource(), event.getType(), event.getId(), e);
		}
	}
}
