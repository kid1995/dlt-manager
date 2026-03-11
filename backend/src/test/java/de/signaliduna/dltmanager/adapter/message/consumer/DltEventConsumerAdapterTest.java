package de.signaliduna.dltmanager.adapter.message.consumer;

import de.signaliduna.dltmanager.core.service.IncomingDltEventManager;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DltEventConsumerAdapterTest {

    @Mock
    IncomingDltEventManager dltEventManager;

    @Mock
    JsonMapper jsonMapper;

    @InjectMocks
    DltEventConsumerAdapter adapter;

    @Test
    void onDltEvent_whenManagerThrowsGenericException_shouldLogAndNotRethrow(){
        CloudEvent event = mock(CloudEvent.class);
        CloudEventData data = mock(CloudEventData.class);
        when(event.getId()).thenReturn("test-id");
        when(event.getData()).thenReturn(data);
        when(data.toBytes()).thenReturn("{}".getBytes(StandardCharsets.UTF_8));
        when(jsonMapper.readValue(any(byte[].class), eq(de.signaliduna.dltmanager.adapter.message.consumer.model.DltEventData.class)))
            .thenReturn(new de.signaliduna.dltmanager.adapter.message.consumer.model.DltEventData(
                "originalId", "svc", null, "topic", "partition", "traceId", "payload", "application/json", "err", "stack"));
        doThrow(new RuntimeException("unexpected")).when(dltEventManager).onDltEvent(any());

        adapter.dltEventReceived().accept(event);

        verify(dltEventManager).onDltEvent(any());
    }

    @Test
    void onDltEvent_whenJacksonExceptionHasNullLocation_shouldLogUnknownLocation(){
        CloudEvent event = mock(CloudEvent.class);
        CloudEventData data = mock(CloudEventData.class);
        when(event.getId()).thenReturn("test-id");
        when(event.getData()).thenReturn(data);
        when(data.toBytes()).thenReturn("bad".getBytes(StandardCharsets.UTF_8));
        tools.jackson.core.exc.StreamReadException ex = mock(tools.jackson.core.exc.StreamReadException.class);
        when(ex.getLocation()).thenReturn(null);
        when(jsonMapper.readValue(any(byte[].class), eq(de.signaliduna.dltmanager.adapter.message.consumer.model.DltEventData.class)))
            .thenThrow(ex);

        adapter.dltEventReceived().accept(event);

        verifyNoInteractions(dltEventManager);
    }
}
