package de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker.http;

import org.springframework.http.HttpStatus;

import java.util.Set;

public class RecoverableHttpErrorCodes {
	private static final Set<Integer> DEFAULT_RECOVERABLE_HTTP_ERROR_CODES = Set.of(
		HttpStatus.SERVICE_UNAVAILABLE.value(),
		HttpStatus.REQUEST_TIMEOUT.value(),
		HttpStatus.GATEWAY_TIMEOUT.value(),
		HttpStatus.BAD_GATEWAY.value()
	);

	private final Set<Integer> codes;

	public RecoverableHttpErrorCodes() {
		this(DEFAULT_RECOVERABLE_HTTP_ERROR_CODES);
	}

	public RecoverableHttpErrorCodes(Set<Integer> codes) {
		this.codes = codes;
	}

	public boolean isRecoverable(int httpErrorCode) {
		return codes.contains(httpErrorCode);
	}
}
