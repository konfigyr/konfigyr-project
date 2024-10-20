package com.konfigyr.security;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountStatus;
import com.konfigyr.account.Memberships;
import com.konfigyr.entity.EntityId;
import com.konfigyr.security.authority.MembershipAuthoritiesConverter;
import com.konfigyr.support.Avatar;
import lombok.*;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.types.Identifiable;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;

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
@Builder
@AggregateRoot
public class AccountPrincipal implements OAuth2User, UserDetails, Identifiable<EntityId>, Serializable {

	@Serial
	private static final long serialVersionUID = -4208974779066630762L;

	private static final String EMPTY_PASSWORD = "";
	private static final Map<String, Object> EMPTY_ATTRIBUTES = Collections.emptyMap();

	/**
	 * Entity identifier of the {@link Account} behind this {@link AccountPrincipal}.
	 */
	@NonNull
	EntityId id;

	/**
	 * The current status of the {@link Account} that is used to check if this
	 * {@link AccountPrincipal} can be used.
	 */
	@NonNull
	AccountStatus status;

	/**
	 * E-Mail address of the {@link AccountPrincipal}.
	 */
	@NonNull
	String email;

	/**
	 * Display name, or an email address, of this {@link AccountPrincipal}.
	 */
	@NonNull
	String name;

	/**
	 * Avatar that points to the profile picture of this {@link AccountPrincipal}.
	 */
	@NonNull
	Avatar avatar;

	/**
	 * Namespace memberships for this {@link AccountPrincipal}, can not be {@code null}.
	 */
	@NonNull
	Memberships memberships;

	@NonNull
	Collection<? extends GrantedAuthority> authorities;

	/**
	 * Creates a new {@link AccountPrincipal} using the attributes from the {@link Account} entity.
	 *
	 * @param account account behind the principal
	 * @return authenticated account principal, never {@literal null}
	 * @throws IllegalArgumentException when account is {@literal null}
	 */
	@NonNull
	public static AccountPrincipal from(Account account) {
		Assert.notNull(account, "Account cano not be null");

		return builder()
				.id(account.id())
				.status(account.status())
				.email(account.email())
				.name(account.displayName())
				.avatar(account.avatar())
				.memberships(account.memberships())
				.authorities(MembershipAuthoritiesConverter.getInstance().convert(account.memberships()))
				.build();
	}

	/**
	 * Returns an {@link EntityId} of the {@link Account} that would be used as the
	 * {@link org.springframework.security.core.AuthenticatedPrincipal}.
	 *
	 * @return account entity identifier, never {@literal null}
	 */
	@NonNull
	@Identity
	public EntityId getId() {
		return id;
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
		return id.serialize();
	}

	@Override
	public String getUsername() {
		return getName();
	}

	/**
	 * The email address of the logged-in user account.
	 *
	 * @return account email address, never {@literal null}
	 */
	@NonNull
	public String getEmail() {
		return email;
	}

	/**
	 * The display name for the logged-in user account, if present, or the email account.
	 *
	 * @return display name or email, never {@literal null}
	 */
	@NonNull
	public String getDisplayName() {
		return name;
	}

	/**
	 * Avatar for the {@link Account user account} is hosted.
	 *
	 * @return avatar URL, can't be {@literal null}
	 */
	@NonNull
	public Avatar getAvatar() {
		return avatar;
	}

	@Override
	public boolean isAccountNonExpired() {
		return AccountStatus.DEACTIVATED != status;
	}

	@Override
	public boolean isAccountNonLocked() {
		return AccountStatus.SUSPENDED != status;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return AccountStatus.ACTIVE == status;
	}

	@Override
	public boolean isEnabled() {
		return AccountStatus.ACTIVE == status;
	}

	@Override
	public String getPassword() {
		return EMPTY_PASSWORD;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return EMPTY_ATTRIBUTES;
	}

}
