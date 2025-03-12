package com.konfigyr.security;

import lombok.Value;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OAuthScopesTest {

	@Test
	@DisplayName("should create an empty scope set")
	void empty() {
		assertThatObject(OAuthScopes.empty())
				.isNotNull()
				.returns(0, OAuthScopes::size)
				.returns(true, OAuthScopes::isEmpty)
				.returns(List.of(), OAuthScopes::toAuthorities)
				.isSameAs(OAuthScopes.empty())
				.isSameAs(OAuthScopes.from(null))
				.isSameAs(OAuthScopes.parse(null))
				.isSameAs(OAuthScopes.of((Collection<OAuthScope>) null))
				.isSameAs(OAuthScopes.of(Set.of()))
				.isSameAs(OAuthScopes.of((OAuthScope[]) null))
				.hasSameHashCodeAs(OAuthScopes.empty())
				.hasToString("");
	}

	@Test
	@DisplayName("should ignore nulls when creating OAuth scope set")
	void ignoreNulls() {
		assertThatObject(OAuthScopes.of(OAuthScope.OPENID, null, OAuthScope.NAMESPACES))
				.isNotNull()
				.returns(2, OAuthScopes::size)
				.returns(false, OAuthScopes::isEmpty)
				.returns(List.of(OAuthScope.OPENID, OAuthScope.NAMESPACES), OAuthScopes::toAuthorities);
	}

	@Test
	@DisplayName("should fail to parse scopes set from an empty JWT")
	void emptyClaims() {
		final var claims = mock(ClaimAccessor.class);

		assertThatObject(OAuthScopes.from(claims))
				.isNotNull()
				.returns(0, OAuthScopes::size)
				.returns(true, OAuthScopes::isEmpty)
				.returns(List.of(), OAuthScopes::toAuthorities)
				.isSameAs(OAuthScopes.empty())
				.hasSameHashCodeAs(OAuthScopes.empty())
				.hasToString("");

		verify(claims).hasClaim("scp");
		verify(claims).hasClaim("scope");

		verify(claims, never()).getClaimAsString(anyString());
	}

	@Test
	@DisplayName("should return a sorted list of OAuth Scopes as Granted Authorities")
	void assertSortedAuthorities() {
		final var scopes = OAuthScopes.of(
				OAuthScope.READ_NAMESPACES,
				OAuthScope.DELETE_NAMESPACES,
				OAuthScope.OPENID,
				OAuthScope.WRITE_NAMESPACES,
				OAuthScope.INVITE_MEMBERS
		);

		assertThat(scopes.toAuthorities())
				.allMatch(OAuthScope.class::isInstance)
				.asInstanceOf(InstanceOfAssertFactories.iterable(OAuthScope.class))
				.contains(
						OAuthScope.OPENID,
						OAuthScope.READ_NAMESPACES,
						OAuthScope.WRITE_NAMESPACES,
						OAuthScope.DELETE_NAMESPACES,
						OAuthScope.INVITE_MEMBERS
				);
	}

	@Test
	@DisplayName("should sort scopes by ordinal when creating string representation of OAuth scopes")
	void assertStringRepresentation() {
		assertThat(OAuthScopes.of(OAuthScope.READ_NAMESPACES, OAuthScope.DELETE_NAMESPACES, OAuthScope.OPENID))
				.hasToString("openid namespaces:read namespaces:delete");
	}

	@Test
	@DisplayName("should assert identity and equality of OAuth scopes")
	void assertIdentityAndEquality() {
		final var scopes = OAuthScopes.of(OAuthScope.READ_NAMESPACES, OAuthScope.DELETE_NAMESPACES, OAuthScope.OPENID);

		assertThatObject(scopes)
				.returns(
						List.of(OAuthScope.OPENID, OAuthScope.READ_NAMESPACES, OAuthScope.DELETE_NAMESPACES),
						OAuthScopes::toAuthorities
				)
				.isEqualTo(
						OAuthScopes.of(OAuthScope.DELETE_NAMESPACES, OAuthScope.READ_NAMESPACES, OAuthScope.OPENID)
				)
				.hasSameHashCodeAs(
						OAuthScopes.of(OAuthScope.OPENID, OAuthScope.DELETE_NAMESPACES, OAuthScope.READ_NAMESPACES)
				);
	}

	@Test
	@DisplayName("should check if granted authority is present in the set")
	void containsGrantedAuthority() {
		final var scopes = OAuthScopes.of(OAuthScope.WRITE_NAMESPACES, OAuthScope.INVITE_MEMBERS);

		assertThat(scopes.contains(OAuthScope.READ_NAMESPACES)).isTrue();
		assertThat(scopes.contains(OAuthScope.WRITE_NAMESPACES)).isTrue();
		assertThat(scopes.contains(OAuthScope.INVITE_MEMBERS)).isTrue();
		assertThat(scopes.contains(OAuthScope.DELETE_NAMESPACES)).isFalse();
		assertThat(scopes.contains(OAuthScope.NAMESPACES)).isFalse();
	}

	@Test
	@DisplayName("should check if granted authority is permitted by the set")
	void permitsGrantedAuthority() {
		final var scopes = OAuthScopes.of(OAuthScope.DELETE_NAMESPACES, OAuthScope.INVITE_MEMBERS);

		assertThat(scopes.permits(OAuthScope.READ_NAMESPACES)).isFalse();
		assertThat(scopes.permits(OAuthScope.WRITE_NAMESPACES)).isFalse();
		assertThat(scopes.permits(OAuthScope.INVITE_MEMBERS)).isTrue();
		assertThat(scopes.permits(OAuthScope.DELETE_NAMESPACES)).isTrue();
		assertThat(scopes.permits(OAuthScope.NAMESPACES)).isTrue();
	}

	@Test
	@DisplayName("should fail to check if unsupported scope is included")
	void containsUnsupportedScope() {
		final var scopes = OAuthScopes.of(OAuthScope.OPENID);

		assertThatExceptionOfType(InvalidOAuthScopeException.class)
				.isThrownBy(() -> scopes.contains("invalid"));

		assertThatExceptionOfType(InvalidOAuthScopeException.class)
				.isThrownBy(() -> scopes.contains(new SimpleGrantedAuthority("invalid")));
	}

	@Test
	@DisplayName("should check if null scopes are contained")
	void containsNullScopes() {
		final var scopes = OAuthScopes.of(OAuthScope.OPENID);

		assertThat(scopes.contains(OAuthScope.OPENID.getAuthority())).isTrue();
		assertThat(scopes.contains((GrantedAuthority) null)).isFalse();
		assertThat(scopes.contains((OAuthScope) null)).isFalse();
		assertThat(scopes.contains((String) null)).isFalse();
		assertThat(scopes.contains("")).isFalse();
		assertThat(scopes.contains(" ")).isFalse();
	}

	@Test
	@DisplayName("should fail to check if unsupported scope is permitted")
	void permitsUnsupportedScope() {
		final var scopes = OAuthScopes.of(OAuthScope.OPENID);

		assertThatExceptionOfType(InvalidOAuthScopeException.class)
				.isThrownBy(() -> scopes.permits("invalid"));

		assertThatExceptionOfType(InvalidOAuthScopeException.class)
				.isThrownBy(() -> scopes.permits(new SimpleGrantedAuthority("invalid")));
	}

	@Test
	@DisplayName("should check if null scopes are permitted")
	void permitsNullScopes() {
		final var scopes = OAuthScopes.of(OAuthScope.OPENID);

		assertThat(scopes.permits(OAuthScope.OPENID.getAuthority())).isTrue();
		assertThat(scopes.permits((GrantedAuthority) null)).isFalse();
		assertThat(scopes.permits((OAuthScope) null)).isFalse();
		assertThat(scopes.permits((String) null)).isFalse();
		assertThat(scopes.permits("")).isFalse();
		assertThat(scopes.permits(" ")).isFalse();
	}

	@Test
	@DisplayName("should parse granted authority")
	void parseGrantedAuthority() {
		assertThat(OAuthScopes.forGrantedAuthority(null, scope -> true)).isFalse();
		assertThat(OAuthScopes.forGrantedAuthority(new SimpleGrantedAuthority("SCOPE_"), scope -> true)).isFalse();

		assertThat(OAuthScopes.forGrantedAuthority(
				OAuthScope.READ_NAMESPACES,
				OAuthScope.READ_NAMESPACES::equals
		)).isTrue();

		assertThat(OAuthScopes.forGrantedAuthority(
				new SimpleGrantedAuthority("namespaces:read"),
				OAuthScope.READ_NAMESPACES::equals
		)).isTrue();

		assertThat(OAuthScopes.forGrantedAuthority(
				new SimpleGrantedAuthority("SCOPE_namespaces:invite"),
				OAuthScope.INVITE_MEMBERS::equals
		)).isTrue();
	}

	@Test
	@DisplayName("should fail to parse unknown scope")
	void parseUnknownScope() {
		assertThatExceptionOfType(InvalidOAuthScopeException.class)
				.isThrownBy(() -> OAuthScopes.parse("openid unknown"))
				.withMessageContaining("Invalid OAuth scope of: unknown")
				.withNoCause()
				.extracting(OAuth2AuthenticationException::getError)
				.returns(OAuth2ErrorCodes.INVALID_SCOPE, OAuth2Error::getErrorCode)
				.returns("Invalid OAuth scope of: unknown", OAuth2Error::getDescription)
				.returns(null, OAuth2Error::getUri);
	}

	@MethodSource("scopes")
	@ParameterizedTest(name = "should parse \"{0}\" scopes to: \"{1}\"")
	@DisplayName("should extract scopes from JWT claims accessor as string value")
	void parseScopesFromClaimsAccessorAsString(List<String> value, OAuthScopes expected) {
		final var claims = TestingAccessor.string(value);

		assertThatObject(OAuthScopes.from(claims))
				.as("should parse `scope` JWT claim into OAuthScopes from values: %s", value)
				.isEqualTo(expected);
	}

	@MethodSource("scopes")
	@ParameterizedTest(name = "should parse \"{0}\" scopes to: \"{1}\"")
	@DisplayName("should extract scopes from JWT claims accessor as list value")
	void parseScopesFromClaimsAccessorAsList(List<String> value, OAuthScopes expected) {
		final var claims = TestingAccessor.list(value);

		assertThatObject(OAuthScopes.from(claims))
				.as("should parse `scope` JWT claim into OAuthScopes from values: %s", value)
				.isEqualTo(expected);
	}

	static Stream<Arguments> scopes() {
		return Stream.of(
				Arguments.of(
						null,
						OAuthScopes.empty()
				),
				Arguments.of(
						List.of("   "),
						OAuthScopes.empty()
				),
				Arguments.of(
						List.of(),
						OAuthScopes.empty()
				),
				Arguments.of(
						List.of("openid"),
						OAuthScopes.of(OAuthScope.OPENID)
				),
				Arguments.of(
						List.of("openid", "namespaces:read", "namespaces:delete"),
						OAuthScopes.of(OAuthScope.OPENID, OAuthScope.READ_NAMESPACES, OAuthScope.DELETE_NAMESPACES)
				),
				Arguments.of(
						List.of("namespaces:delete", "namespaces:read", "namespaces:invite"),
						OAuthScopes.of(OAuthScope.DELETE_NAMESPACES, OAuthScope.READ_NAMESPACES, OAuthScope.INVITE_MEMBERS)
				)
		);
	}

	@Value
	static class TestingAccessor implements ClaimAccessor {
		Map<String, Object> claims;

		static ClaimAccessor list(List<String> values) {
			if (CollectionUtils.isEmpty(values)) {
				return new TestingAccessor(Map.of());
			}

			return new TestingAccessor(Map.of("scope", values));
		}

		static ClaimAccessor string(List<String> values) {
			if (CollectionUtils.isEmpty(values)) {
				return new TestingAccessor(Map.of());
			}

			return new TestingAccessor(Map.of("scope", String.join(" ", values)));
		}
	}

}
