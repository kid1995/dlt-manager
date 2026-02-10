package de.signaliduna.elpa.dltmanager.adapter.message.util;

import feign.FeignException;
import feign.Request;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TestDataHelper {

	public static class FeignExceptions {

		public static FeignException.FeignClientException.BadRequest badRequest() {
			return new FeignException.FeignClientException.BadRequest("400 Bad Request", createMockGetRequest(),
				"bad request".getBytes(StandardCharsets.UTF_8), Map.of());
		}

		public static FeignException.FeignClientException.ServiceUnavailable unavailable() {
			return new FeignException.FeignClientException.ServiceUnavailable("503 Service unavailable", createMockGetRequest(),
				"Service unavailable".getBytes(StandardCharsets.UTF_8), Map.of());
		}

		public static Request createMockGetRequest() {
			return Request.create(Request.HttpMethod.GET,
				"http://example.com/test/mock",
				Map.of(),
				Request.Body.create(new byte[]{}),
				null);
		}
	}

	private TestDataHelper() {
	}
}
