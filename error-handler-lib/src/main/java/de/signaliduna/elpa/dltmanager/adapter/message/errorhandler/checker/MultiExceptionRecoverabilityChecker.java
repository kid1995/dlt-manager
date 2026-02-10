package de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker;

import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Allows to assemble a customized {@code ExceptionRecoverabilityChecker} supporting predefined checks
 * (e.g. {@code FeignExceptionRecoverabilityChecker}) as well as additional user defined {@code ExceptionRecoverabilityChecker}s.
 * The isRecoverable result of this class is {@code true} if at least one of the provided checkers returns {@code true}.
 */
public class MultiExceptionRecoverabilityChecker implements ExceptionRecoverabilityChecker {
	private final List<ExceptionRecoverabilityChecker> checkers;

	public MultiExceptionRecoverabilityChecker(List<ExceptionRecoverabilityChecker> checkers) {
		if (CollectionUtils.isEmpty(checkers)) {
			throw new IllegalArgumentException("checkers must not be null or empty");
		}
		this.checkers = checkers;
	}

	@Override
	public final boolean isRecoverable(Throwable throwable) {
		return checkers.stream().anyMatch(checker -> checker.isRecoverable(throwable));
	}
}
