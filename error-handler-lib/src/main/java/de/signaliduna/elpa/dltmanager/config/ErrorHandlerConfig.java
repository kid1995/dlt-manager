package de.signaliduna.elpa.dltmanager.config;

import tools.jackson.databind.ObjectMapper;
import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.VorgangProcessIdExtractor;
import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker.ExceptionRecoverabilityChecker;
import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker.http.FeignExceptionRecoverabilityChecker;
import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker.http.RecoverableHttpErrorCodes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ErrorHandlerConfig {

	@Bean
	@ConditionalOnMissingBean
	public RecoverableHttpErrorCodes recoverableHttpErrorCodes() {
		return new RecoverableHttpErrorCodes();
	}

	@Bean
	@ConditionalOnMissingBean
	public ExceptionRecoverabilityChecker exceptionRecoverabilityChecker(RecoverableHttpErrorCodes recoverableHttpErrorCodes) {
		return new FeignExceptionRecoverabilityChecker(recoverableHttpErrorCodes);
	}

	@Bean
	@ConditionalOnMissingBean
	public VorgangProcessIdExtractor vorgangProcessIdExtractor(ObjectMapper objectMapper) {
		return new VorgangProcessIdExtractor(objectMapper);
	}
}
