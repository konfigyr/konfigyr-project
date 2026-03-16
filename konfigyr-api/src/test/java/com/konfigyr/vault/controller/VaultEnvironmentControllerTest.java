package com.konfigyr.vault.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.Services;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import com.konfigyr.vault.*;
import com.konfigyr.vault.environment.ConfigEnvironment;
import com.konfigyr.vault.environment.PropertySource;
import com.konfigyr.vault.state.GitStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

public class VaultEnvironmentControllerTest extends AbstractControllerTest {

	@Autowired
	VaultProperties properties;

	@Autowired
	ProfileManager profiles;

	@Autowired
	Services services;

	@Autowired
	VaultAccessor accessor;

	@Test
	@DisplayName("should not retrieve configs due insufficient permissions")
	void retrieveConfigsWithoutPermission() {
		mvc.get().uri("/configs/{service}/{profiles}", "john-doe-blog", "live")
				.with(httpBasic("kfg-A2c7mvoxEP346BQCSuwnJ5ZNQIEsgCBG", "n0obEPw2_5DoDNkxyXhW5Ul1TgC-t2r3H8_wj7PDqFc"))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	@DisplayName("should not retrieve configs for an unknown service")
	void retrieveConfigsForUnknownService() {
		mvc.get().uri("/configs/{service}/{profiles}",  "unknown-service", "live")
				.with(httpBasic("kfg-BAQp6u2ElYmuPyoa2Hj766ju0PPvL2Iq", "nryjshWX-PdDHdR8yqyu1u5A2KBFgH-O_ljxbQODo-Y"))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus4xxClientError()
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should retrieve configs for unknown profile")
	void retrieveConfigsForUnknownProfile() {
		mvc.get().uri("/configs/{service}/{profiles}",  "john-doe-blog", "dev")
				.with(httpBasic("kfg-BAQp6u2ElYmuPyoa2Hj766ju0PPvL2Iq", "nryjshWX-PdDHdR8yqyu1u5A2KBFgH-O_ljxbQODo-Y"))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(ConfigEnvironment.class)
				.returns("john-doe-blog", ConfigEnvironment::name)
				.returns(new String[]{"dev"}, ConfigEnvironment::profiles)
				.returns(List.of(), ConfigEnvironment::propertySources);
	}

	@Test
	@DisplayName("should retrieve configs")
	void retrieveConfigs() throws Exception {
		final Service service = services.get(EntityId.from(1)).orElseThrow();
		final Profile profile = updateProfilePolicy(lookupProfile("live", service), ProfilePolicy.UNPROTECTED);
		final GitStateRepository repository = GitStateRepository.initialize(service, properties.getRepositoryDirectory());

		try {
			repository.create(profile);

			try (Vault vault = accessor.open((AuthenticatedPrincipal) TestPrincipals.john().getPrincipal(), service, profile)) {
				vault.apply(PropertyChanges.builder()
						.profile(profile)
						.subject("Test changes")
						.createProperty("server.port", "8080")
						.build()
				);
			}

			mvc.get().uri("/configs/{service}/{profiles}", service.slug(), "live,dev")
					.with(httpBasic("kfg-BAQp6u2ElYmuPyoa2Hj766ju0PPvL2Iq", "nryjshWX-PdDHdR8yqyu1u5A2KBFgH-O_ljxbQODo-Y"))
					.exchange()
					.assertThat()
					.apply(log())
					.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
					.bodyJson()
					.convertTo(ConfigEnvironment.class)
					.returns(service.slug(), ConfigEnvironment::name)
					.returns(new String[]{profile.slug(), "dev"}, ConfigEnvironment::profiles)
					.returns(
							List.of(new PropertySource("john-doe-blog-live", Map.of("server.port", "8080"))),
							ConfigEnvironment::propertySources
					);

		} finally {
			updateProfilePolicy(profile, ProfilePolicy.PROTECTED);
			repository.destroy();
			repository.close();
		}
	}

	private Profile lookupProfile(String name, Service service) {
		return profiles.get(service, name).orElseThrow();
	}

	private Profile updateProfilePolicy(Profile profile, ProfilePolicy policy) {
		return profiles.update(profile.id(), ProfileDefinition.builder()
				.service(profile.service())
				.name(profile.name())
				.slug(profile.slug())
				.description(profile.description())
				.policy(policy)
				.build()
		);
	}
}
