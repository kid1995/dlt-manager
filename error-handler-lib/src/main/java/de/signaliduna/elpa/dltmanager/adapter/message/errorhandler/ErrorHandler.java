package de.signaliduna.elpa.dltmanager.adapter.message.errorhandler;

import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker.ExceptionRecoverabilityChecker;
import de.signaliduna.elpa.dltmanager.adapter.message.producer.DltTopicProducer;
import de.signaliduna.elpa.dltmanager.adapter.message.producer.RetryTopicProducer;
import de.signaliduna.elpa.dltmanager.adapter.message.util.MessagingUtils;
import jakarta.annotation.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Custom error handler class for a Spring Cloud Stream binding that decides if a message that could not be processed
 * successfully will be retried (via a dedicated retry topic) or if it will be published to a dedicated dead letter topic (DLT).
 * The ErrorHandler will be invoked when a message could not be processed after the binding's configured {@code max-attempts} count.
 * To enable this handler, it must be configured via a binding's {@code error-handler-definition} property in the {@code application.yml}).
 */
public class ErrorHandler extends AbstractCustomErrorHandler {
	private final RetryTopicProducer retryTopicProducer;
	private final DltTopicProducer dltTopicProducer;
	private final ExceptionRecoverabilityChecker exceptionRecoverabilityChecker;

	public ErrorHandler(
		RetryTopicProducer retryTopicProducer,
		DltTopicProducer dltTopicProducer,
		IdExtractor idExtractor,
		ExceptionRecoverabilityChecker exceptionRecoverabilityChecker
	) {
		super(idExtractor);
		this.retryTopicProducer = retryTopicProducer;
		this.dltTopicProducer = dltTopicProducer;
		this.exceptionRecoverabilityChecker = exceptionRecoverabilityChecker;
	}

	@Override
	protected void handleMessagingExceptionWithFailedMessage(MessagingException me, Message<?> failedMsg) {
		@Nullable final String eventId = MessagingUtils.idOf(me.getFailedMessage());
		final var exception = getCauseOrThrowable(me);
		String idInfo = getIdInfo(failedMsg);
		if (exceptionRecoverabilityChecker.isRecoverable(exception)) {
			String topicName = retryTopicProducer.getTopicName();
			log.warn("processing of event for {} (eventId: {}) failed, pushing to retry topic {}", idInfo, eventId, topicName, exception);
			retryTopicProducer.publishToRetryTopic(failedMsg.getPayload());
		} else {
			String topicName = dltTopicProducer.getTopicName();
			log.warn("processing of event for {} (eventId: {}) failed, pushing to DLT {}", idInfo, topicName, eventId, exception);
			dltTopicProducer.createAndPublishDltEvent(me, failedMsg);
		}
	}

	static Throwable getCauseOrThrowable(Throwable throwable) {
		return throwable.getCause() != null ? throwable.getCause() : throwable;
	}
}
