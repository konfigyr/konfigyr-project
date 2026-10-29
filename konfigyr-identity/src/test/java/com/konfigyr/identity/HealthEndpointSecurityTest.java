package com.konfigyr.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "management.health.mail.enabled=false")
class HealthEndpointSecurityTest extends AbstractControllerIntegrationTest {

	@Test
	@DisplayName("should allow unauthenticated access to the health endpoint")
	void shouldPermitHealth() {
		assertThat(mvc.get().uri("/actuator/health"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.status")
				.isEqualTo("UP");
	}

	@Test
	@DisplayName("should allow unauthenticated access to the liveness probe")
	void shouldPermitLivenessProbe() {
		assertThat(mvc.get().uri("/actuator/health/liveness"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.status")
				.isEqualTo("UP");
	}

	@Test
	@DisplayName("should allow unauthenticated access to the readiness probe")
	void shouldPermitReadinessProbe() {
		assertThat(mvc.get().uri("/actuator/health/readiness"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.status")
				.isEqualTo("UP");
	}

	@Test
	@DisplayName("should redirect to login for other actuator endpoints")
	void shouldRequireAuthenticationForOtherEndpoints() {
		assertThat(mvc.get().uri("/actuator/info")).hasStatus3xxRedirection();
		assertThat(mvc.get().uri("/actuator/metrics")).hasStatus3xxRedirection();
	}

}
