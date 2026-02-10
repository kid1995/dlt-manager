package de.signaliduna.dltmanager.adapter.http.client;


import de.signaliduna.dltmanager.config.ClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "papierantragEingangClient", configuration = ClientConfig.class)
public interface PapierantragEingangClient {
	@PostMapping("/papierantrag/{papierantragUuid}")
	ResponseEntity<String> resendPapierantrag(@PathVariable final String papierantragUuid);
}
