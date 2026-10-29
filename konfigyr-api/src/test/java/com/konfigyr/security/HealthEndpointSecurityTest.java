package com.konfigyr.security;

import com.konfigyr.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthEndpointSecurityTest extends AbstractControllerTest {

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
	@DisplayName("should require authentication for other actuator endpoints")
	void shouldRequireAuthenticationForOtherEndpoints() {
		assertThat(mvc.get().uri("/actuator/info")).hasStatus(401);
		assertThat(mvc.get().uri("/actuator/metrics")).hasStatus(401);
	}

}
