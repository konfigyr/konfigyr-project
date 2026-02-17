package com.konfigyr.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.konfigyr.artifactory.store.MetadataStore;
import com.konfigyr.feature.Features;
import com.konfigyr.mail.Mailer;
import com.konfigyr.test.smtp.TestSmtpServer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.jspecify.annotations.NonNull;
import org.springframework.modulith.test.PublishedEventsExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.nio.file.Path;

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
@TestSmtpServer
@ImportTestcontainers(TestContainers.class)
@ExtendWith(PublishedEventsExtension.class)
public abstract class AbstractIntegrationTest {

	/**
	 * The {@link WireMockServer} instance that is created by the {@link EnableWireMock} annotation.
	 */
	@InjectWireMock
	protected static WireMockServer wiremock;

	/**
	 * The JUnit Temporary directory that can be used to mock the Java {@link java.nio.file.FileSystem}.
	 */
	@TempDir
	protected static Path tempDirectory;

	/**
	 * The {@link Features} Bean that is being {@link org.mockito.Spy spied} upon by Mockito
	 * to mock various feature testing scenarios.
	 */
	@MockitoSpyBean
	protected Features features;

	/**
	 * The {@link MetadataStore} Bean that is being {@link org.mockito.Spy spied} upon by Mockito
	 * to mock various feature testing scenarios. The default Store Bean is using the in-memory file
	 * system provided by JIMFS, see {@link #tempDirectory}.
	 */
	@MockitoSpyBean
	protected MetadataStore metadataStore;

	/**
	 * The {@link Mailer} Bean that is being {@link org.mockito.Spy spied} upon by Mockito
	 * to intercept and inspect {@link com.konfigyr.mail.Mail Mail messages}.
	 */
	@MockitoSpyBean
	protected Mailer mailer;

	@DynamicPropertySource
	static void registerTestProperties(@NonNull DynamicPropertyRegistry registry) {
		registry.add("konfigyr.artifactory.metadata-store.root", () -> tempDirectory
				.resolve("metadata-store").toUri());
	}

}
