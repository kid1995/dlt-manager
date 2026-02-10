package de.signaliduna.dltmanager;

import de.signaliduna.elpa.jwtadapter.EnableJwtAdapter;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@OpenAPIDefinition(
	info = @Info(
		title = "dlt-manager",
		version = "1.0.0",
		description = "API zur Administration von DLT Kafka events von Services aus der elpa:4 Service-Familie.",
		contact = @Contact(
			name = "erhan yilmaz",
			email = "erhan.yilmaz@signal-iduna.de",
			url = "http://wiki.system.local")),
	servers = {@Server(
		url = "https://develop-dltmanager-example-test-tst.osot.system.local/api"
	)}
)
@SecurityScheme(
	type = SecuritySchemeType.HTTP,
	scheme = "bearer",
	bearerFormat = "JWT",
	name = "bearerToken",
	description = "Ein valider JWT-Token, der dem Format ```xxxx.yyyy.zzzz``` entspricht. (https://jwt.io/)")
@SpringBootApplication
@EnableFeignClients
@EnableJwtAdapter
public class DltManagerApplication {
	public static void main(final String[] args) {
		SpringApplication.run(DltManagerApplication.class, args);
	}
}
