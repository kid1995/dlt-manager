package de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker;

import java.util.List;

@FunctionalInterface
public interface ExceptionRecoverabilityChecker {
	boolean isRecoverable(Throwable throwable);

	default ExceptionRecoverabilityChecker and(ExceptionRecoverabilityChecker checkers) {
		return new MultiExceptionRecoverabilityChecker(List.of(this, checkers));
	}
}

