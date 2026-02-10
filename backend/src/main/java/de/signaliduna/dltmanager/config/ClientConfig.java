package de.signaliduna.dltmanager.config;

import de.signaliduna.elpa.jwtadapter.core.JwtAdapter;
import de.signaliduna.elpa.jwtadapter.core.JwtInterceptor;
import org.springframework.context.annotation.Bean;

public class ClientConfig {
	@Bean
	public JwtInterceptor jwtInterceptor(JwtAdapter jwtAdapter) {
		return new JwtInterceptor(jwtAdapter);
	}
}
