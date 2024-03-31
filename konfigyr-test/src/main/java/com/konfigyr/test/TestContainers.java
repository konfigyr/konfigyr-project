package com.konfigyr.test;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

/**
 * Utility class that is used to register {@link org.testcontainers.containers.Container test containers}
 * in the Spring text context.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see Container
 * @see ServiceConnection
 * @see org.springframework.boot.testcontainers.context.ImportTestcontainers
 */
@SuppressWarnings("checkstyle:InterfaceIsType")
public interface TestContainers {

	/**
	 * The {@link PostgreSQLContainer PostgreSQL Database container} to be registered in the Spring
	 * test context as a {@link ServiceConnection} to configure the {@link javax.sql.DataSource}.
	 */
	@Container
	@ServiceConnection
	PostgreSQLContainer<?> postresql = new PostgreSQLContainer<>("postgres:16.2");

}
