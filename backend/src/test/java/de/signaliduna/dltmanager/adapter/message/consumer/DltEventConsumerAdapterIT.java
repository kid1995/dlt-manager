package de.signaliduna.dltmanager.adapter.message.consumer;

import de.signaliduna.dltmanager.adapter.db.DltEventRepository;
import de.signaliduna.dltmanager.test.ContainerImageNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.function.core.FunctionInvocationHelper;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;


import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
	properties = {"com.c4-soft.springaddons.oidc.resourceserver.enabled=false", "jwt.enable=false" }
)
@Testcontainers
@Import(TestChannelBinderConfiguration.class)
class DltEventConsumerAdapterIT {

	@Container
	@ServiceConnection
	static final MongoDBContainer mongoDBContainer = new MongoDBContainer(ContainerImageNames.MONGO.getImageName());

	@Value("${topics.elpa-dlt}")
	String elpaDltTopic;

	@Autowired
	DltEventRepository dltEventRepo;

	@Autowired
	FunctionInvocationHelper<Message<?>> functionInvocationHelper;

	@Autowired
	JsonMapper jsonMapper;

	@Autowired
	InputDestination inputDestination;

	@Autowired
	DltEventConsumerAdapter adapter;


	@AfterEach
	void afterEach() {
		// Requirement: Use MongoRepository instead of MongoOperations
		dltEventRepo.deleteAll();
	}

	@Test
	void testIncomingDltEventWithValidPayload() {
		final String cloudEventJson = """
			{
			   "payload" : "eyJvcmlnaW5hbEV2ZW50SWQiOiJjN2VlNjQ4My05ZThmLTFjNzAtMjJkMy05ODg1NTE5MTJlODMiLCJzZXJ2aWNlTmFtZSI6InBhcnRuZXJzeW5jIiwiYWRkVG9EbHRUaW1lc3RhbXAiOlsyMDI0LDcsMTAsMTEsMTAsMzYsODE4MDc5MDAwXSwidHJhY2VJZCI6ImU3NmM1ZWYyNDVlNmMyOTMzYmZhNWFmNDI1MzY1NDZhIiwicGF5bG9hZCI6IntcIm1ldGFkYXRhXCI6e1wicm9oZGF0ZW5VdWlkXCI6XCJpZFwifSxcInByb2Nlc3NJZFwiOlwiaWQxMjNcIn0iLCJwYXlsb2FkTWVkaWFUeXBlIjoiYXBwbGljYXRpb24vanNvbiIsImVycm9yIjoiZXJyb3JNZXNzYWdlIiwic3RhY2tUcmFjZSI6ImRlLnNpZ25hbGlkdW5hLnBhcnRuZXJzeW5jLmNvcmUuZXhjZXB0aW9uLkV4dGVybmFsU2VydmljZUVycm9yRXhjZXB0aW9uOiBlcnJvck1lc3NhZ2UifQ==",
			   "headers" : {
			     "ce_type" : "de.signaliduna.elpa.partnersync.stream.onApplicationReview-in.error",
			     "ce_source" : "/signal-iduna/elpa/partnersync",
			     "message-type" : "cloudevent",
			     "ce_specversion" : "1.0",
			     "ce_id" : "e858107f-139a-46ad-b554-9b51331ddcbd",
			     "contentType" : "application/json"
			   }
			 }""";

		final Message<?> message = msgFromJsonString(cloudEventJson);
		inputDestination.send(message, elpaDltTopic);

		assertThat(dltEventRepo.findAllByOrderByLastAdminActionDesc()).hasSize(1);
	}

	@Test
	void testIncomingDltEventWithNullData() {
		final String cloudEventJson = """
			{
			   "payload" : null,
			   "headers" : {
			     "ce_type" : "test.type",
			     "ce_source" : "test.source",
			     "ce_specversion" : "1.0",
			     "ce_id" : "test-id-null"
			   }
			 }""";

		final Message<?> message = msgFromJsonString(cloudEventJson);
		inputDestination.send(message, elpaDltTopic);

		assertThat(dltEventRepo.findAllByOrderByLastAdminActionDesc()).isEmpty();
	}

	@Test
	void testIncomingDltEventWithInvalidJsonData() {
		final String cloudEventJson = """
			{
			   "payload" : "bm90LWpzb24=",
			   "headers" : {
			     "ce_type" : "test.type",
			     "ce_source" : "test.source",
			     "ce_specversion" : "1.0",
			     "ce_id" : "test-id-invalid",
			     "contentType" : "application/json"
			   }
			 }""";

		final Message<?> message = msgFromJsonString(cloudEventJson);
		inputDestination.send(message, elpaDltTopic);

		assertThat(dltEventRepo.findAllByOrderByLastAdminActionDesc()).isEmpty();
	}

	@Test
	void testAdapterIgnoresCloudEventWithNullDataDirectInvocation() {
		// data == null -> should hit log.warn + return (your missed branch)
		CloudEvent event = createTestCloudEvent("test-id-null", null);

		adapter.dltEventReceived().accept(event);

		assertThat(dltEventRepo.findAllByOrderByLastAdminActionDesc()).isEmpty();
	}

	@Test
	void testAdapterHandlesInvalidJsonDataDirectInvocation() {
		CloudEvent event = createTestCloudEvent(
			"test-id-invalid",
			"not-json".getBytes(StandardCharsets.UTF_8)
		);

		adapter.dltEventReceived().accept(event);

		assertThat(dltEventRepo.findAllByOrderByLastAdminActionDesc()).isEmpty();
	}




	private Message<?> msgFromJsonString(String jsonString) {
		try {
			final var dto = jsonMapper.readValue(jsonString, MessageDto.class);
			byte[] payload = dto.payload == null ? null : Base64.getDecoder().decode(dto.payload);

			return MessageBuilder.withPayload(payload == null ? new byte[0] : payload)
				.copyHeaders(dto.headers)
				.build();
		} catch (JacksonException e) {
			throw new RuntimeException(e);
		}
	}

	private record MessageDto(String payload, Map<String, Object> headers) {}

	private static CloudEvent createTestCloudEvent(String id, byte[] data) {
		var builder = CloudEventBuilder.v1()
			.withId(id)
			.withType("test.type")
			.withSource(URI.create("test.source"));

		if (data != null) {
			builder = builder.withData("application/json", data);
		}

		return builder.build();
	}




}

