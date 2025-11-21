package com.konfigyr.identity.authentication;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Map;

/**
 * Implementation of the {@link AccountIdentityUser} that is able to load, or create, the {@link AccountIdentity}
 * from the retrieved {@link OidcUser}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Value
@RequiredArgsConstructor
class OidcAccountIdentityUser implements AccountIdentityUser, OidcUser {

	AccountIdentity accountIdentity;
	OidcUser delegate;

	@Override
	public Map<String, Object> getClaims() {
		return delegate.getClaims();
	}

	@Override
	public OidcUserInfo getUserInfo() {
		return delegate.getUserInfo();
	}

	@Override
	public OidcIdToken getIdToken() {
		return delegate.getIdToken();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return delegate.getAttributes();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return accountIdentity.getAuthorities();
	}
}
