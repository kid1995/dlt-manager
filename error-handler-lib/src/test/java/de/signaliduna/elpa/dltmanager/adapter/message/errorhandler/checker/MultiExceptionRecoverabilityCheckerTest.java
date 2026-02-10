package de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker;

import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker.http.FeignExceptionRecoverabilityChecker;
import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker.http.RecoverableHttpErrorCodes;
import de.signaliduna.elpa.dltmanager.adapter.message.util.TestDataHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiExceptionRecoverabilityCheckerTest {
	@Test
	void shouldThrowIllegalArgumentExceptionWhenCheckersListIsNull() {
		assertThatThrownBy(() -> new MultiExceptionRecoverabilityChecker(null))
			.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("checkers must not be null or empty");
	}

	@Test
	void shouldThrowIllegalArgumentExceptionWhenCheckersListIsEmpty() {
		assertThatThrownBy(() -> new MultiExceptionRecoverabilityChecker(List.of()))
			.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("checkers must not be null or empty");
	}

	private static List<IsRecoverableSpec> shouldWorkWithTwoExceptionCheckers() {
		return List.of(
			new IsRecoverableSpec(new MyCustomException(true), true),
			new IsRecoverableSpec(new MyCustomException(false), false),
			new IsRecoverableSpec(TestDataHelper.FeignExceptions.unavailable(), true),
			new IsRecoverableSpec(TestDataHelper.FeignExceptions.badRequest(), false),
			new IsRecoverableSpec(new RuntimeException("exception without checker instance"), false)
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldWorkWithTwoExceptionCheckers(IsRecoverableSpec spec) {
		final var multiChecker = new MultiExceptionRecoverabilityChecker(List.of(
			new FeignExceptionRecoverabilityChecker(new RecoverableHttpErrorCodes()),
			ex -> ex instanceof MyCustomException myCustomException && myCustomException.isRecoverableError
		));
		assertThat(multiChecker.isRecoverable(spec.exception)).isEqualTo(spec.expectedResult);
	}

	record IsRecoverableSpec(Throwable exception, boolean expectedResult) {
	}
}

class MyCustomException extends RuntimeException {
	public final boolean isRecoverableError;

	public MyCustomException(boolean isRecoverableError) {
		this.isRecoverableError = isRecoverableError;
	}

	@Override
	public String toString() {
		return "MyCustomException(isRecoverableError=%s)".formatted(isRecoverableError);
	}
}
