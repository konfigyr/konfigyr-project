package com.konfigyr.security.oauth;

import com.konfigyr.account.Account;
import com.konfigyr.entity.EntityId;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * {@link OAuth2User} implementation that bridges the gap between the original {@link OAuth2User} and
 * the {@link Account}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Value
@RequiredArgsConstructor
public class OAuth2UserAccount implements OAuth2User, Serializable {

	@Serial
	private static final long serialVersionUID = -4208974779066630762L;

	private static final Map<String, Object> EMPTY_ATTRIBUTES = Collections.emptyMap();

	Account account;

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

	@NonNull
	public String getEmail() {
		return account.email();
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
	public Map<String, Object> getAttributes() {
		return EMPTY_ATTRIBUTES;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return AuthorityUtils.createAuthorityList("admin");
	}

}
