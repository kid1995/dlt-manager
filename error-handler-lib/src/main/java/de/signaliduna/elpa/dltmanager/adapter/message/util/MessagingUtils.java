package de.signaliduna.elpa.dltmanager.adapter.message.util;

import jakarta.annotation.Nullable;
import org.springframework.util.CollectionUtils;

public class MessagingUtils {

	@Nullable
	public static <T> String idOf(org.springframework.messaging.Message<T> message) {
		if (message == null || CollectionUtils.isEmpty(message.getHeaders())) {
			return null;
		}
		final var id = message.getHeaders().get("id");
		return (id == null) ? null : id.toString();
	}

	private MessagingUtils() {
	}
}
