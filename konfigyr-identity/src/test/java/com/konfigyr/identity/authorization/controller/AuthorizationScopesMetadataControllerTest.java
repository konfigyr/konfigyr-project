package com.konfigyr.identity.authorization.controller;

import com.konfigyr.identity.authorization.controller.AuthorizationScopesMetadataController.ScopeMetadata;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

@TestProfile
@SpringBootTest
@AutoConfigureMockMvc
@ImportTestcontainers(TestContainers.class)
class AuthorizationScopesMetadataControllerTest {

	@Autowired
	MockMvcTester mvc;

	@Test
	@DisplayName("should redirect to login page when not authenticated")
	void shouldRedirectToLogin() {
		final var request = get("/oauth/scope-metadata");

		assertThat(mvc.perform(request))
				.apply(log())
				.hasStatus3xxRedirection()
				.hasRedirectedUrl("/login");
	}

	@Test
	@DisplayName("should render registered OAuth scope metadata")
	void shouldFailToFindRegisteredOAuthClient() {
		final var request = get("/oauth/scope-metadata")
				.with(authenticated());

		mvc.perform(request)
				.assertThat()
				.hasStatusOk()
				.bodyJson()
				.convertTo(InstanceOfAssertFactories.iterable(ScopeMetadata.class))
				.containsExactlyInAnyOrder(
						new ScopeMetadata(
								OAuthScope.OPENID.getAuthority(),
						"Scope used to indicate that the application intends to use OIDC to verify the user's identity."
						),
						new ScopeMetadata(
								OAuthScope.NAMESPACES.getAuthority(),
								"Grants full access to namespace management that includes read, write and delete access to namespaces and managing invitations and collaborators."
						),
						new ScopeMetadata(
								OAuthScope.INVITE_MEMBERS.getAuthority(),
								"Grants read access to namespaces and the possibility to manage invitations and collaborators."
						),
						new ScopeMetadata(OAuthScope.READ_NAMESPACES.getAuthority(), "Grants read-only access to namespaces."),
						new ScopeMetadata(OAuthScope.WRITE_NAMESPACES.getAuthority(), "Grants read and write access to namespaces."),
						new ScopeMetadata(OAuthScope.DELETE_NAMESPACES.getAuthority(), "Grants read, write and delete access to namespaces."),
						new ScopeMetadata(
								OAuthScope.PROFILES.getAuthority(),
								"Grants full access to service profile configuration management that includes read,write and delete access to profiles."
						),
						new ScopeMetadata(OAuthScope.READ_PROFILES.getAuthority(), "Grants read-only access to profile."),
						new ScopeMetadata(OAuthScope.WRITE_PROFILES.getAuthority(), "Grants read and write access to profile."),
						new ScopeMetadata(OAuthScope.DELETE_PROFILES.getAuthority(), "Grants read, write and delete access to profile.")
				);
	}

	static RequestPostProcessor authenticated() {
		return SecurityMockMvcRequestPostProcessors.jwt();
	}

}
