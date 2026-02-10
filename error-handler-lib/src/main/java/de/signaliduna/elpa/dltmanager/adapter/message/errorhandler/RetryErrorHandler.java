package de.signaliduna.elpa.dltmanager.adapter.message.errorhandler;

import de.signaliduna.elpa.dltmanager.adapter.message.producer.DltTopicProducer;
import de.signaliduna.elpa.dltmanager.adapter.message.util.MessagingUtils;
import jakarta.annotation.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Custom error handler class for a Spring Cloud Stream binding that is intended to be used with retry topics.
 * The handler will be invoked when an event could not be processed successfully even after the configured {@code max-attempts}
 * count and will publish the event to a dead letter topic (DLT).
 * To enable this handler, it must be configured via a binding's {@code error-handler-definition} property in the {@code application.yml}).
 */
public class RetryErrorHandler extends AbstractCustomErrorHandler {
	private final DltTopicProducer dltTopicProducer;

	public RetryErrorHandler(DltTopicProducer dltTopicProducer, IdExtractor idExtractor) {
		super(idExtractor);
		this.dltTopicProducer = dltTopicProducer;
	}

	@Override
	protected void handleMessagingExceptionWithFailedMessage(MessagingException me, Message<?> failedMsg) {
		@Nullable final String eventId = MessagingUtils.idOf(me.getFailedMessage());
		String idInfo = getIdInfo(failedMsg);
		String topicName = dltTopicProducer.getTopicName();
		log.warn("retry processing of event for {} (eventId: {}) failed, pushing to DLT {}", idInfo, eventId, topicName, me);
		dltTopicProducer.createAndPublishDltEvent(me, failedMsg);
	}
}
