package de.signaliduna.dltmanager.utils;

import feign.FeignException;
import feign.Request;
import jakarta.annotation.Nullable;
import org.jspecify.annotations.NonNull;

/**
 * Extracts safe, PII-free information from exceptions for logging and error responses.
 */
public final class SafeExceptionLogger {
    
    private SafeExceptionLogger() {
    }
    
    /**
     * Extracts only safe metadata from a FeignException
     */
    public static String sanitizeFeignException(FeignException e) {
        return "FeignException{status=%d, method=%s, url=%s}".formatted(
                e.status(),
                e.request() != null ? e.request().httpMethod() : "unknown",
                sanitizeUrl(e.request())
        );
    }
    
    public static String safeClassName(Throwable e) {
        return e.getClass().getSimpleName();
    }

    /**
     * Strips newlines and control characters from user-supplied values
     * to prevent log injection (javasecurity:S5145).
     */
    public static String sanitizeLogArg(@Nullable String value) {
        if (value == null) {
            return "null";
        }
        return value.replaceAll("[\r\n\t\\p{Cntrl}]", "_");
    }
    
    // remove query params to make sure
    private static @NonNull String sanitizeUrl(@Nullable Request request) {
        if (request == null) {
            return "unknown";
        }
        String url = request.url();
        int queryIndex = url.indexOf('?');
        return queryIndex > 0 ? url.substring(0, queryIndex) : url;
    }
}
