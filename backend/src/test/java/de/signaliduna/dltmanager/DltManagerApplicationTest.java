package de.signaliduna.dltmanager;

import com.c4_soft.springaddons.security.oauth2.test.webmvc.AutoConfigureAddonsWebmvcResourceServerSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
	"SERVICE_SYSTEM=TEST",
	"AUTH_PASSWORD=pw",
	"com.c4-soft.springaddons.oidc.resourceserver.enabled=false"
})
@AutoConfigureAddonsWebmvcResourceServerSecurity
@Import(TestChannelBinderConfiguration.class)
class DltManagerApplicationTest {
	@Test
	void contextLoads(ApplicationContext context) {
		assertThat(context).isNotNull();
	}
}
