package com.konfigyr.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.modulith.test.PublishedEventsExtension;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

/**
 * Abstract test class for integration tests that would start the application with the {@link TestProfile test}
 * Spring active profile and register the needed {@link TestContainers service test containers}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@TestProfile
@SpringBootTest
@EnableWireMock
@ImportTestcontainers(TestContainers.class)
@ExtendWith(PublishedEventsExtension.class)
public abstract class AbstractIntegrationTest {

	/**
	 * The {@link WireMockServer} instance that is created by the {@link EnableWireMock} annotation.
	 */
	@InjectWireMock
	protected WireMockServer wiremock;

}
