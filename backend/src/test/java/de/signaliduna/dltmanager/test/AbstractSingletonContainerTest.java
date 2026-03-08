package de.signaliduna.dltmanager.test;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Abstract base class for integration tests using Testcontainers that should share the same container instances
 * (See
 * <a href="https://testcontainers.com/guides/testcontainers-container-lifecycle/#_using_singleton_containers">
 *   https://testcontainers.com
 * </a>).
 */
@Testcontainers
public abstract class AbstractSingletonContainerTest {
    @ServiceConnection
    @Container
    protected static final PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer(
        DockerImageName.parse(ContainerImageNames.POSTGRES.getImageName()).asCompatibleSubstituteFor(PostgreSQLContainer.IMAGE)
    );
}
