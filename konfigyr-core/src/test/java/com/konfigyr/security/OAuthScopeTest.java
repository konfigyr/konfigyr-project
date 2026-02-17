package com.konfigyr.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class OAuthScopeTest {

	@Test
	@DisplayName("should resolve OAuth scope from value")
	void resolve() {
		assertThat(OAuthScope.from("namespaces:write"))
				.isEqualTo(OAuthScope.WRITE_NAMESPACES);
	}

	@Test
	@DisplayName("should fail to resolve OAuth scope from value")
	void invalidValue() {
		assertThatIllegalArgumentException().isThrownBy(() -> OAuthScope.from(null));
		assertThatExceptionOfType(InvalidOAuthScopeException.class).isThrownBy(() -> OAuthScope.from(""));
		assertThatExceptionOfType(InvalidOAuthScopeException.class).isThrownBy(() -> OAuthScope.from(" "));
		assertThatExceptionOfType(InvalidOAuthScopeException.class).isThrownBy(() -> OAuthScope.from("unknown"));
	}

	@Test
	@DisplayName("should parse scopes")
	void parse() {
		assertThat(OAuthScope.parse(null))
				.isNotNull()
				.isEmpty();

		assertThat(OAuthScope.parse(""))
				.isNotNull()
				.isEmpty();

		assertThat(OAuthScope.parse(" "))
				.isNotNull()
				.isEmpty();

		assertThat(OAuthScope.parse("namespaces:write namespaces:delete namespaces:invite"))
				.containsExactlyInAnyOrder(
						OAuthScope.WRITE_NAMESPACES,
						OAuthScope.DELETE_NAMESPACES,
						OAuthScope.INVITE_MEMBERS
				);
	}

	@Test
	@DisplayName("should fail to parse invalid scopes")
	void parseInvalidScope() {
		assertThatExceptionOfType(InvalidOAuthScopeException.class)
				.isThrownBy(() -> OAuthScope.parse("namespaces:write unknown"))
				.withMessageContaining("Invalid OAuth scope of: unknown")
				.withNoCause()
				.extracting(OAuth2AuthenticationException::getError)
				.returns(OAuth2ErrorCodes.INVALID_SCOPE, OAuth2Error::getErrorCode)
				.returns("Invalid OAuth scope of: unknown", OAuth2Error::getDescription)
				.returns(null, OAuth2Error::getUri);
	}

	@MethodSource("scenarios")
	@DisplayName("should validate structure for OAuth scopes")
	@ParameterizedTest(name = "permission for {0} - {1} - {2}")
	void sanityTest(OAuthScope scope, String label, Set<OAuthScope> included) {
		assertThat(scope)
				.returns(label, OAuthScope::getAuthority)
				.returns(included, OAuthScope::getIncluded);
	}

	static Stream<Arguments> scenarios() {
		return Stream.of(
				/* OpenID */
				Arguments.of(OAuthScope.OPENID, "openid", Set.of()),

				/* Namespace scope group */
				Arguments.of(OAuthScope.READ_NAMESPACES, "namespaces:read", Set.of()),
				Arguments.of(OAuthScope.WRITE_NAMESPACES, "namespaces:write", Set.of(
						OAuthScope.READ_NAMESPACES
				)),
				Arguments.of(OAuthScope.DELETE_NAMESPACES, "namespaces:delete", Set.of(
						OAuthScope.READ_NAMESPACES, OAuthScope.WRITE_NAMESPACES
				)),
				Arguments.of(OAuthScope.INVITE_MEMBERS, "namespaces:invite", Set.of(
						OAuthScope.READ_NAMESPACES
				)),
				Arguments.of(OAuthScope.NAMESPACES, "namespaces", Set.of(
						OAuthScope.READ_NAMESPACES,
						OAuthScope.WRITE_NAMESPACES,
						OAuthScope.DELETE_NAMESPACES,
						OAuthScope.INVITE_MEMBERS
				)),

				/* Profile scope group */
				Arguments.of(OAuthScope.READ_PROFILES, "profiles:read", Set.of()),
				Arguments.of(OAuthScope.WRITE_PROFILES, "profiles:write", Set.of(
						OAuthScope.READ_PROFILES
				)),
				Arguments.of(OAuthScope.DELETE_PROFILES, "profiles:delete", Set.of(
						OAuthScope.READ_PROFILES, OAuthScope.WRITE_PROFILES
				)),
				Arguments.of(OAuthScope.NAMESPACES, "profiles", Set.of(
						OAuthScope.READ_PROFILES, OAuthScope.WRITE_PROFILES, OAuthScope.DELETE_PROFILES
				))
		);
	}

}
