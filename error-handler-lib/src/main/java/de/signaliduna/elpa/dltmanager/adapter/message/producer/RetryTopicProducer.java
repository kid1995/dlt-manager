package de.signaliduna.elpa.dltmanager.adapter.message.producer;

import org.springframework.cloud.stream.function.StreamBridge;

public class RetryTopicProducer {
	private final StreamBridge streamBridge;
	private final String retryBindingName;
	private final String topicName;

	public RetryTopicProducer(StreamBridge streamBridge, String retryBindingName, String topicName) {
		this.streamBridge = streamBridge;
		this.retryBindingName = retryBindingName;
		this.topicName = topicName;
	}

	public String getTopicName() {
		return topicName;
	}

	public void publishToRetryTopic(Object failedMessagePayload) {
		streamBridge.send(retryBindingName, failedMessagePayload);
	}

}
