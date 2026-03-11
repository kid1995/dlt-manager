package de.signaliduna.dltmanager.adapter.message.consumer.mapper;

import de.signaliduna.dltmanager.adapter.message.consumer.model.DltEventData;
import de.signaliduna.dltmanager.core.model.DltEvent;
import io.cloudevents.CloudEvent;

import java.util.UUID;

public class EventDataMapper {


	public static DltEvent toDomainObject(CloudEvent event, DltEventData eventData){
		return new DltEvent(
			UUID.fromString(event.getId()),
			eventData.originalEventId(),
			eventData.serviceName(),
			eventData.addToDltTimestamp(),
			eventData.topic(),
			eventData.partition(),
			eventData.traceId(),
			eventData.payload(),
			eventData.payloadMediaType(),
			eventData.error(),
			eventData.stackTrace(),
			null
		);
	}


	private EventDataMapper() {
	}
}
