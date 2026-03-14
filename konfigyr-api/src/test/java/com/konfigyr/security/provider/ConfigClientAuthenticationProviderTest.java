package com.konfigyr.security.provider;

import com.konfigyr.security.basic.BasicAuthenticatedPrincipal;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;
import static com.konfigyr.data.tables.OauthApplications.OAUTH_APPLICATIONS;

@ExtendWith(MockitoExtension.class)
class ConfigClientAuthenticationProviderTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	DSLContext context;

	PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	ConfigClientAuthenticationProvider provider;

	@BeforeEach
	void setup() {
		provider = new ConfigClientAuthenticationProvider(context, passwordEncoder);
		provider.afterPropertiesSet();
	}

	@Test
	@DisplayName("should fail to authenticate due missing credentials")
	void shouldFailToAuthenticateDueMissingCredentials() {
		final Authentication authentication = new UsernamePasswordAuthenticationToken("client-1", null);

		assertThrows(BadCredentialsException.class, () -> provider.authenticate(authentication));
	}

	@Test
	@DisplayName("should fail to authenticate due expired application")
	void shouldFailToAuthenticateWhenApplicationIsExpired() {
		final Authentication authentication = new UsernamePasswordAuthenticationToken("app", "secret");

		mockLookupApplication("secret", OffsetDateTime.now().minusDays(1));

		assertThrows(CredentialsExpiredException.class, () -> provider.authenticate(authentication));
	}

	@Test
	@DisplayName("should fail to authenticate due incorrect secret")
	void shouldFailToAuthenticateDueIncorrectSecret() {
		final Authentication authentication = new UsernamePasswordAuthenticationToken("app", "secret");

		mockLookupApplication(passwordEncoder.encode("incorrect secret"), OffsetDateTime.now().plusDays(1));

		assertThrows(BadCredentialsException.class, () -> provider.authenticate(authentication));
	}

	@Test
	@DisplayName("should successfully authenticate")
	void shouldSuccessfullyAuthenticate() {
		final Authentication authentication = new UsernamePasswordAuthenticationToken("app", "secret");

		mockLookupApplication(passwordEncoder.encode("secret"), OffsetDateTime.now().plusDays(1));

		assertThat(provider.authenticate(authentication))
				.isNotNull()
				.returns(true, Authentication::isAuthenticated)
				.returns(null, Authentication::getCredentials)
				.returns(new BasicAuthenticatedPrincipal("app", "n1"), Authentication::getPrincipal)
				.extracting(Authentication::getAuthorities, InstanceOfAssertFactories.list(GrantedAuthority.class))
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("profiles", "profiles:read", "profiles:write", "profiles:delete", "namespaces:read");
	}

	void mockLookupApplication(String secret, OffsetDateTime expires) {
		final var app = new ConfigClientAuthenticationProvider.Application(
				"app", secret, expires, "profiles namespaces:read", "n1"
		);

		when(
				context.select(
								OAUTH_APPLICATIONS.CLIENT_ID,
								OAUTH_APPLICATIONS.CLIENT_SECRET,
								OAUTH_APPLICATIONS.EXPIRES_AT,
								OAUTH_APPLICATIONS.SCOPES,
								NAMESPACES.SLUG
						)
						.from(OAUTH_APPLICATIONS)
						.leftJoin(NAMESPACES)
						.on(NAMESPACES.ID.eq(OAUTH_APPLICATIONS.NAMESPACE_ID))
						.where(OAUTH_APPLICATIONS.CLIENT_ID.eq("app"))
						.fetchOneInto(ConfigClientAuthenticationProvider.Application.class)
		).thenReturn(app);
	}
}
