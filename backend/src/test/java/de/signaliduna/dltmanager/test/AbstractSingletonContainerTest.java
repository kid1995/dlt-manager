package de.signaliduna.dltmanager.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;


/**
 * Abstract base class for integration tests using Testcontainers that should share the same container instances
 * (See <a href="http://google.com">https://testcontainers.com/guides/testcontainers-container-lifecycle/#_using_singleton_containers</a>).
 */
public abstract class AbstractSingletonContainerTest {

	@Container
	protected static final MongoDBContainer MONGODB_CONTAINER = new MongoDBContainer(DockerImageName.parse("hub.docker.system.local/mongo:6").asCompatibleSubstituteFor("mongo"));

	protected static final Logger log = LoggerFactory.getLogger(AbstractSingletonContainerTest.class);

	static {
		MONGODB_CONTAINER.start();
	}

	@DynamicPropertySource
	static void mongodbProperties(DynamicPropertyRegistry registry) {
		MONGODB_CONTAINER.start();
		registry.add("spring.data.mongodb.uri", MONGODB_CONTAINER::getReplicaSetUrl);
	}
}
