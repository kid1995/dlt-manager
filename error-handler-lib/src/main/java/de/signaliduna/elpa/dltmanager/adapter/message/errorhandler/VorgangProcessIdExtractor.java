package de.signaliduna.elpa.dltmanager.adapter.message.errorhandler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Extracts the {@code processId} from a {@code de.signaliduna.elpa.sharedlib.model.Vorgang} payload object.
 * Note: {@code VorgangProcessIdExtractor} directly accesses the payload JSON instead of using the elpa4-model library
 * for extracting the {@code processId} in order to keep the dependencies of the error-handler-lib as small as possible
 * and to avoid version conflicts.
 */
public class VorgangProcessIdExtractor implements IdExtractor {
	private final ObjectMapper objectMapper;

	public VorgangProcessIdExtractor(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String idName() {
		return "processId";
	}

	@Override
	public String extractId(Message<?> message) {
		final var payload = message.getPayload();
		return switch (payload) {
			case byte[] ba -> this.extractVorgangProcessId(ba);
			case String str -> this.extractVorgangProcessId(str.getBytes(StandardCharsets.UTF_8));
			default -> extractVorgangProcessId(asJsonBytes(payload));
		};
	}

	byte[] asJsonBytes(Object payload) {
		try {
			return objectMapper.writeValueAsBytes(payload);
		} catch (IOException e) {
			throw new IllegalArgumentException("failed write value as JSON bytes", e);
		}
	}

	String extractVorgangProcessId(byte[] payload) {
		try {
			final JsonNode jsonNode = objectMapper.readTree(payload);
			final var processIdNode = jsonNode.get(this.idName());
			if (processIdNode == null) {
				throw new IllegalArgumentException("failed to extract Vorgang.%s: field not found".formatted(this.idName()));
			}
			return processIdNode.asString();
		} catch (IOException e) {
			throw new IllegalArgumentException("failed to read VorgangStub", e);
		}
	}
}
