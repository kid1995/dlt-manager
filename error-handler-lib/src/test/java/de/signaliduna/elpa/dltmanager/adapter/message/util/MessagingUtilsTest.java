package de.signaliduna.elpa.dltmanager.adapter.message.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessagingUtilsTest {

	@Mock
	Message<String> message;

	@Test
	void idOfShouldBeNullSafe() {
		assertThat(MessagingUtils.idOf(null)).isNull();

		when(message.getHeaders()).thenReturn(null);
		assertThat(MessagingUtils.idOf(message)).isNull();

		final var messageHeaders = mock(MessageHeaders.class);
		when(messageHeaders.get(any())).thenReturn(null);
		when(message.getHeaders()).thenReturn(messageHeaders);
		assertThat(MessagingUtils.idOf(message)).isNull();
	}
}
