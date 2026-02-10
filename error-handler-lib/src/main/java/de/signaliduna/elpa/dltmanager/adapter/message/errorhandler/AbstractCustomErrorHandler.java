package de.signaliduna.elpa.dltmanager.adapter.message.errorhandler;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;

import java.util.function.Consumer;

public abstract class AbstractCustomErrorHandler implements Consumer<ErrorMessage> {
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	protected final IdExtractor idExtractor;

	protected AbstractCustomErrorHandler(IdExtractor idExtractor) {
		this.idExtractor = idExtractor;
	}

	/**
	 * Abstract handler method with @{code me.getFailedMessage() == failedMsg != null} guaranteed for the given {@link MessagingException}.
	 */
	protected abstract void handleMessagingExceptionWithFailedMessage(MessagingException me, Message<?> failedMsg);

	@Override
	public void accept(ErrorMessage errorMessage) {
		if (errorMessage.getPayload() instanceof MessagingException me) {
			final Message<?> failedMsg = me.getFailedMessage();
			if (failedMsg == null) {
				logUnexpectedMessage("messagingException.getFailedMessage() message is null", me);
				return;
			}
			handleMessagingExceptionWithFailedMessage(me, me.getFailedMessage());
			return;
		}
		final var payloadInfo = errorMessage.getPayload().getClass().getName();
		logUnexpectedMessage("accept - expected errorMessage.getPayload() to be a MessagingException but was %s".formatted(payloadInfo), null);
	}

	/**
	 * For testing
	 */
	void logUnexpectedMessage(String message, @Nullable Exception exception) {
		log.error(message, exception);
	}

	protected String getIdInfo(Message<?> message) {
		try {
			return idExtractor.labeledIdValueOf(message);
		} catch (Exception e) {
			log.debug("failed to extract id from failedMsg", e);
			return "<failed to extract id>";
		}
	}
}
