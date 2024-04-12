package com.konfigyr.security;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountStatus;
import com.konfigyr.entity.EntityId;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Class that represents the {@link org.springframework.security.core.AuthenticatedPrincipal}
 * that retrieves all the security attributes from the {@link Account}.
 * <p>
 * It is recommended that every {@link org.springframework.security.core.Authentication} type
 * that is created by Spring Security should contain this {@link AccountPrincipal} as it's subject.
 * <p>
 * {@link AccountPrincipal} implements both {@link OAuth2User} and {@link UserDetails} on order
 * to be compatible with Spring OAuth authentication types and authentication types that are
 * using the {@link org.springframework.security.core.userdetails.UserDetailsService} API.
 * <p>
 * Keep in mind that the <code>username</code> for this principal is the external representation
 * of the {@link EntityId account entity identifier} and that the <code>password</code> is intentionally
 * set to a <strong>blank string</strong> as {@link Account accounts} do not have one.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see OAuth2User
 * @see UserDetails
 **/
@Value
@RequiredArgsConstructor
public class AccountPrincipal implements OAuth2User, UserDetails, Serializable {

	@Serial
	private static final long serialVersionUID = -4208974779066630762L;

	private static final String EMPTY_PASSWORD = "";
	private static final Map<String, Object> EMPTY_ATTRIBUTES = Collections.emptyMap();

	/**
	 * The actual {@link Account} that identifies this {@link AccountPrincipal}.
	 */
	@NonNull Account account;

	/**
	 * Returns am {@link EntityId} of the {@link Account} that would be used as the
	 * {@link org.springframework.security.core.AuthenticatedPrincipal}.
	 *
	 * @return account entity identifier, never {@literal null}
	 */
	@NonNull
	public EntityId getId() {
		return account.id();
	}

	/**
	 * Returns the serialized external form the {@link EntityId} of the {@link Account} as the
	 * name of the {@link org.springframework.security.core.AuthenticatedPrincipal}.
	 *
	 * @return the authenticated principal name, never {@literal null}
	 */
	@NonNull
	@Override
	public String getName() {
		return account.id().serialize();
	}

	/**
	 * The email address of the logged-in user account.
	 *
	 * @return account email address, never {@literal null}
	 */
	@NonNull
	public String getEmail() {
		return account.email();
	}

	@Override
	public String getUsername() {
		return getName();
	}

	/**
	 * The display name for the logged-in user account, if present, or the email account.
	 *
	 * @return display name or email, never {@literal null}
	 */
	@NonNull
	public String getDisplayName() {
		return account.displayName();
	}

	/**
	 * URL where the avatar for the {@link Account user account} is hosted.
	 *
	 * @return avatar URL, can be {@literal null}
	 */
	@Nullable
	public String getAvatar() {
		return account.avatar();
	}

	@Override
	public boolean isAccountNonExpired() {
		return AccountStatus.DEACTIVATED != account.status();
	}

	@Override
	public boolean isAccountNonLocked() {
		return AccountStatus.SUSPENDED != account.status();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return AccountStatus.ACTIVE == account.status();
	}

	@Override
	public boolean isEnabled() {
		return AccountStatus.ACTIVE == account.status();
	}

	@Override
	public String getPassword() {
		return EMPTY_PASSWORD;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return EMPTY_ATTRIBUTES;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return AuthorityUtils.createAuthorityList("admin");
	}

}
