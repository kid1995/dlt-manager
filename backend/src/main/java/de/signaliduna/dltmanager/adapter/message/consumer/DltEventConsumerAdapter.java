package de.signaliduna.dltmanager.adapter.message.consumer;

import de.signaliduna.dltmanager.adapter.message.consumer.mapper.EventDataMapper;
import de.signaliduna.dltmanager.adapter.message.consumer.model.DltEventData;
import de.signaliduna.dltmanager.core.model.DltEvent;
import de.signaliduna.dltmanager.core.service.IncomingDltEventManager;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.util.function.Consumer;

@Component
public class DltEventConsumerAdapter {
	private static final Logger log = LoggerFactory.getLogger(DltEventConsumerAdapter.class);
	private final IncomingDltEventManager dltEventManager;
	private final JsonMapper jsonMapper;

	public DltEventConsumerAdapter(IncomingDltEventManager dltEventManager, JsonMapper jsonMapper) {
		this.dltEventManager = dltEventManager;
		this.jsonMapper = jsonMapper;
	}

	@Bean
	public Consumer<CloudEvent> dltEventReceived() {
		return this::onDltEvent;
	}

	private void onDltEvent(CloudEvent event) {
		try {
			final CloudEventData data = event.getData();
			if (data == null) {
				log.warn("Received CloudEvent without data (id={})", event.getId());
				return;
			}
			final DltEventData eventData = jsonMapper.readValue(data.toBytes(), DltEventData.class);
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
