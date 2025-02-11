package com.konfigyr.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.ClaimAccessor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

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
	@DisplayName("should parse scope set from JWT")
	void claims() {
		final var claims = mock(ClaimAccessor.class);
		doReturn(true).when(claims).hasClaim("scope");
		doReturn("openid namespaces:read namespaces:delete").when(claims).getClaimAsString("scope");

		final var scopes = OAuthScopes.from(claims);

		assertThatObject(scopes)
				.isNotNull()
				.returns(3, OAuthScopes::size)
				.returns(false, OAuthScopes::isEmpty)
				.hasToString("openid namespaces:read namespaces:delete")
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

		assertThat(scopes).containsExactlyInAnyOrder(
				OAuthScope.OPENID,
				OAuthScope.READ_NAMESPACES,
				OAuthScope.DELETE_NAMESPACES
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

		assertThatIllegalArgumentException()
				.isThrownBy(() -> scopes.contains("invalid"));

		assertThatIllegalArgumentException()
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

		assertThatIllegalArgumentException()
				.isThrownBy(() -> scopes.permits("invalid"));

		assertThatIllegalArgumentException()
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

}
