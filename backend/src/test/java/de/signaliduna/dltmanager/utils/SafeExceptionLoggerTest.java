package de.signaliduna.dltmanager.utils;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SafeExceptionLoggerTest {

    @Test
    void sanitizeFeignException_withUrlContainingQueryParams() {
        var request = Request.create(Request.HttpMethod.POST, "http://example.com/api?token=secret", Map.of(), Request.Body.create(new byte[]{}), null);
        var ex = new FeignException.ServiceUnavailable("msg", request, new byte[]{}, Map.of());

        String result = SafeExceptionLogger.sanitizeFeignException(ex);

        assertThat(result).isEqualTo("FeignException{status=503, method=POST, url=http://example.com/api}");
    }

    @Test
    void sanitizeFeignException_withNullRequest() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(503);
        when(ex.request()).thenReturn(null);

        String result = SafeExceptionLogger.sanitizeFeignException(ex);

        assertThat(result).isEqualTo("FeignException{status=503, method=unknown, url=unknown}");
    }
}
