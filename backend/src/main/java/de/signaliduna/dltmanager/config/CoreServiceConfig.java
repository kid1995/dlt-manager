package de.signaliduna.dltmanager.config;

import de.signaliduna.dltmanager.adapter.db.DltEventPersistenceAdapter;
import de.signaliduna.dltmanager.adapter.http.client.PapierantragEingangAdapter;
import de.signaliduna.dltmanager.core.service.DltEventAdminService;
import de.signaliduna.dltmanager.core.service.IncomingDltEventManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class CoreServiceConfig {

	@Bean
	public IncomingDltEventManager incomingDltEventManager(DltEventPersistenceAdapter dltEventPersistenceAdapter) {
		return new IncomingDltEventManager(dltEventPersistenceAdapter);
	}

	@Bean
	public DltEventAdminService dltEventAdminService(
		DltEventPersistenceAdapter dltEventPersistenceService, PapierantragEingangAdapter papierantragEingangAdapter, JsonMapper jsonMapper) {
		return new DltEventAdminService(dltEventPersistenceService, papierantragEingangAdapter, jsonMapper);
	}
}
