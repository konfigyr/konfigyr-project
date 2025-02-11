package com.konfigyr.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
		assertThatIllegalArgumentException().isThrownBy(() -> OAuthScope.from(""));
		assertThatIllegalArgumentException().isThrownBy(() -> OAuthScope.from(" "));
		assertThatIllegalArgumentException().isThrownBy(() -> OAuthScope.from("unknown"));
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
		assertThatIllegalArgumentException().isThrownBy(() -> OAuthScope.parse("namespaces:write unknown"));
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
				))
		);
	}

}
