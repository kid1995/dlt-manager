package de.signaliduna.dltmanager.config;

import de.signaliduna.dltmanager.adapter.http.client.PapierantragEingangAdapter;
import de.signaliduna.dltmanager.adapter.http.client.PapierantragEingangClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdapterConfig {

	@Bean
	public PapierantragEingangAdapter papierantragEingangAdapter(PapierantragEingangClient papierantragEingangClient) {
		return new PapierantragEingangAdapter(papierantragEingangClient);
	}

}
