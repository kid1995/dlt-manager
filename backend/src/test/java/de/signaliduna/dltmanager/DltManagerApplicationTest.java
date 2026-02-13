package de.signaliduna.dltmanager;

import de.signaliduna.dltmanager.test.AbstractSingletonContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
class DltManagerApplicationTest extends AbstractSingletonContainerTest {
	@Test
	void contextLoads(ApplicationContext context) {
		assertThat(context).isNotNull();
	}
}
