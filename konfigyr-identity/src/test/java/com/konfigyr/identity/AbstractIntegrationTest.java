package com.konfigyr.identity;

import com.konfigyr.identity.authorization.issuer.TrustedIssuerRepository;
import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@TestProfile
@SpringBootTest
@ImportTestcontainers(TestContainers.class)
public abstract class AbstractIntegrationTest {

	/**
	 * Mocks the {@link TrustedIssuerRepository} to ease mocking of the well-known issuers.
	 */
	@MockitoSpyBean
	protected TrustedIssuerRepository trustedIssuerRepository;

}
