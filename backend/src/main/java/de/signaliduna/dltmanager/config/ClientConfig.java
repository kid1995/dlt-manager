package de.signaliduna.dltmanager.config;

import de.signaliduna.elpa.jwtadapter.core.JwtAdapter;
import de.signaliduna.elpa.jwtadapter.core.JwtInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

public class ClientConfig {
	@Bean
	@ConditionalOnBean(JwtAdapter.class)
	public JwtInterceptor jwtInterceptor(JwtAdapter jwtAdapter) {
		return new JwtInterceptor(jwtAdapter);
	}
}
