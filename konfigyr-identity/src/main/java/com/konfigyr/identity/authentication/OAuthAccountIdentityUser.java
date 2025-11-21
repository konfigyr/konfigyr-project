package com.konfigyr.identity.authentication;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Implementation of the {@link AccountIdentityUser} that is able to load, or create, the {@link AccountIdentity}
 * from the retrieved {@link OAuth2User}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Value
@RequiredArgsConstructor
public class OAuthAccountIdentityUser implements AccountIdentityUser, OAuth2User  {

	AccountIdentity accountIdentity;
	OAuth2User delegate;

	@Override
	public Map<String, Object> getAttributes() {
		return delegate.getAttributes();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return accountIdentity.getAuthorities();
	}

}
