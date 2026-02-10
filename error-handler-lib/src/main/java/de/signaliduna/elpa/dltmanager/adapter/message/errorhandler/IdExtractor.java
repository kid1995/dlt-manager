package de.signaliduna.elpa.dltmanager.adapter.message.errorhandler;

import org.springframework.messaging.Message;

/**
 * Allows custom error handler classes to extract the id of a message without the need to know details about the
 * message payload.
 */
public interface IdExtractor {
	/**
	 * The name if the field with the id for the {@link #labeledIdValueOf(Message)}.
	 * @return the name of the field with the id.
	 */
	String idName();

	/**
	 * Extracts an id from the payload of the given message for logging purposes.
	 */
	String extractId(Message<?> message);

	/**
	 * To create meaningful log messages without the need to know details about the
	 * message payload classes.
	 */
	default String labeledIdValueOf(Message<?> message) {
		return "%s=%s".formatted(idName(), extractId(message));
	}
}
