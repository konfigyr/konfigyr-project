package com.konfigyr.identity.authentication;

import lombok.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.*;

/**
 * Implementation of the {@link AccountIdentityUser} that is able to load, or create, the {@link AccountIdentity}
 * from the retrieved {@link OidcUser}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Value
class OidcAccountIdentityUser implements AccountIdentityUser, OidcUser {

	AccountIdentity accountIdentity;
	OidcUser delegate;
	Collection<? extends GrantedAuthority> authorities;

	OidcAccountIdentityUser(AccountIdentity accountIdentity, OidcUser delegate) {
		this.accountIdentity = accountIdentity;
		this.delegate = delegate;

		// the OIDC granted authority is never added to the authentication by the provider
		// for us to store the factor issue time, we need to create one here
		final Set<GrantedAuthority> authorities = new LinkedHashSet<>(accountIdentity.getAuthorities());
		authorities.add(FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.AUTHORIZATION_CODE_AUTHORITY));

		this.authorities = Collections.unmodifiableCollection(authorities);
	}

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

}
