package de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker.http;

import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker.ExceptionRecoverabilityChecker;
import feign.FeignException;

public class FeignExceptionRecoverabilityChecker implements ExceptionRecoverabilityChecker {
	private final RecoverableHttpErrorCodes recoverableHttpErrorCodes;

	public FeignExceptionRecoverabilityChecker(RecoverableHttpErrorCodes recoverableHttpErrorCodes) {
		this.recoverableHttpErrorCodes = recoverableHttpErrorCodes;
	}

	@Override
	public boolean isRecoverable(Throwable throwable) {
		if (throwable instanceof FeignException feignException) {
			return recoverableHttpErrorCodes.isRecoverable(feignException.status());
		}
		return false;
	}
}
