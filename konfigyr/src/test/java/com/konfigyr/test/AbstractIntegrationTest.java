package com.konfigyr.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

/**
 * Abstract test class for integration tests that would start the application with the {@link TestProfile test}
 * Spring active profile and register the needed {@link TestContainers service test containers}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@TestProfile
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ImportTestcontainers(TestContainers.class)
public abstract class AbstractIntegrationTest {
}
