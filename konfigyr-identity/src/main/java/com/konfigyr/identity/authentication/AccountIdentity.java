package com.konfigyr.identity.authentication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Avatar;
import lombok.Builder;
import lombok.Value;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.types.Identifiable;
import org.springframework.lang.NonNull;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;

/**
 * Class that represents the {@link org.springframework.security.core.AuthenticatedPrincipal}
 * that retrieves the security attributes from the stored user accounts.
 * <p>
 * It is recommended that every {@link org.springframework.security.core.Authentication} type
 * that is created by Spring Security should contain either {@link AccountIdentity} or
 * {@link AccountIdentityUser} as it's subject.
 * <p>
 * Keep in mind that the <code>username</code> for this principal is the external representation
 * of the {@link EntityId account entity identifier} and that the <code>password</code> is intentionally
 * set to a <strong>blank string</strong> as our user accounts do not have one.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see UserDetails
 * @see AccountIdentityUser
 **/
@Value
@Builder
@AggregateRoot
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountIdentity implements UserDetails, AuthenticatedPrincipal, Identifiable<EntityId>, Serializable {

	@Serial
	private static final long serialVersionUID = -4208974779066630762L;

	private static final String EMPTY_PASSWORD = "";

	/**
	 * Entity identifier of the user account behind this {@link AccountIdentity}.
	 */
	@NonNull
	EntityId id;

	/**
	 * The current status of the account that is used to check if this {@link AccountIdentity} can be used.
	 */
	@NonNull
	AccountIdentityStatus status;

	/**
	 * E-Mail address of the {@link AccountIdentity}.
	 */
	@NonNull
	String email;

	/**
	 * Display name, or an email address, of this {@link AccountIdentity}.
	 */
	@NonNull
	String displayName;

	/**
	 * Avatar that points to the profile picture of this {@link AccountIdentity}.
	 */
	@NonNull
	Avatar avatar;

	@NonNull
	Collection<? extends GrantedAuthority> authorities;

	/**
	 * Returns an {@link EntityId} of the {@link AccountIdentity} that would be used as the
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
	 * Returns the serialized external form the {@link EntityId} of the {@link AccountIdentity} as the
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
	@JsonIgnore
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
		return displayName;
	}

	/**
	 * Avatar for the {@link AccountIdentity}.
	 *
	 * @return account avatar, can't be {@literal null}
	 */
	@NonNull
	public Avatar getAvatar() {
		return avatar;
	}

	@Override
	public boolean isAccountNonExpired() {
		return AccountIdentityStatus.DEACTIVATED != status;
	}

	@Override
	public boolean isAccountNonLocked() {
		return AccountIdentityStatus.SUSPENDED != status;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return AccountIdentityStatus.ACTIVE == status;
	}

	@Override
	public boolean isEnabled() {
		return AccountIdentityStatus.ACTIVE == status;
	}

	@Override
	@JsonIgnore
	public String getPassword() {
		return EMPTY_PASSWORD;
	}

	@NonNull
	@Override
	public String toString() {
		return "AccountIdentity(id=" + id + ", status=" + status + ")";
	}

}
