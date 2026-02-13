package de.signaliduna.elpa.dltmanager.config;

import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.VorgangProcessIdExtractor;
import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker.ExceptionRecoverabilityChecker;
import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker.http.FeignExceptionRecoverabilityChecker;
import de.signaliduna.elpa.dltmanager.adapter.message.errorhandler.checker.http.RecoverableHttpErrorCodes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

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
	public VorgangProcessIdExtractor vorgangProcessIdExtractor(JsonMapper jsonMapper) {
		return new VorgangProcessIdExtractor(jsonMapper);
	}
}
