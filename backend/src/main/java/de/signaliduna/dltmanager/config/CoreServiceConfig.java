package de.signaliduna.dltmanager.config;

import tools.jackson.databind.ObjectMapper;
import de.signaliduna.dltmanager.adapter.db.DltEventPersistenceAdapter;
import de.signaliduna.dltmanager.adapter.http.client.PapierantragEingangAdapter;
import de.signaliduna.dltmanager.core.service.DltEventAdminService;
import de.signaliduna.dltmanager.core.service.IncomingDltEventManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreServiceConfig {

	@Bean
	public IncomingDltEventManager incomingDltEventManager(DltEventPersistenceAdapter dltEventPersistenceAdapter) {
		return new IncomingDltEventManager(dltEventPersistenceAdapter);
	}

	@Bean
	public DltEventAdminService dltEventAdminService(
		DltEventPersistenceAdapter dltEventPersistenceService, PapierantragEingangAdapter papierantragEingangAdapter, ObjectMapper objectMapper) {
		return new DltEventAdminService(dltEventPersistenceService, papierantragEingangAdapter, objectMapper);
	}
}
