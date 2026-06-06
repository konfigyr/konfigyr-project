package com.konfigyr.identity.authentication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Avatar;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.types.Identifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

/**
 * Class that represents the {@link AuthenticatedPrincipal} that retrieves the security attributes
 * from the stored user accounts.
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
@NullMarked
@AggregateRoot
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AccountIdentity implements UserDetails, AuthenticatedPrincipal, Identifiable<EntityId>, Serializable {

	@Serial
	private static final long serialVersionUID = -4208974779066630762L;

	private static final String EMPTY_PASSWORD = "";

	private final EntityId id;
	private final AccountIdentityStatus status;
	private final String email;
	private final String displayName;
	private final Avatar avatar;
	private final Collection<? extends GrantedAuthority> authorities;

	private AccountIdentity(
			EntityId id,
			AccountIdentityStatus status,
			String email,
			String displayName,
			Avatar avatar,
			Collection<? extends GrantedAuthority> authorities
	) {
		this.id = id;
		this.status = status;
		this.email = email;
		this.displayName = displayName;
		this.avatar = avatar;
		this.authorities = authorities;
	}

	/**
	 * Creates a new {@link Builder fluent account identity builder} instance used to create
	 * the {@link AccountIdentity}.
	 *
	 * @return account identity builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Returns the {@link EntityId} of the user account behind this {@link AccountIdentity}.
	 *
	 * @return account entity identifier, never {@literal null}
	 */
	@Identity
	public EntityId getId() {
		return id;
	}

	/**
	 * Returns the current status of this {@link AccountIdentity}.
	 *
	 * @return account identity status, never {@literal null}
	 */
	public AccountIdentityStatus getStatus() {
		return status;
	}

	/**
	 * Returns the e-mail address of this {@link AccountIdentity}.
	 *
	 * @return e-mail address, never {@literal null}
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Returns the display name, or an email address, of this {@link AccountIdentity}.
	 *
	 * @return display name, never {@literal null}
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the {@link Avatar} that points to the profile picture of this {@link AccountIdentity}.
	 *
	 * @return account avatar, never {@literal null}
	 */
	public Avatar getAvatar() {
		return avatar;
	}

	/**
	 * Returns the serialized external form of the {@link EntityId} of the {@link AccountIdentity}
	 * as the name of the {@link AuthenticatedPrincipal}.
	 *
	 * @return the authenticated principal name, never {@literal null}
	 */
	@Override
	public String getName() {
		return id.serialize();
	}

	@Override
	@JsonIgnore
	public String getUsername() {
		return getName();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
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

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AccountIdentity that)) {
			return false;
		}
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	@Override
	public String toString() {
		return "AccountIdentity(id=" + id + ", status=" + status + ")";
	}

	/**
	 * Fluent builder type used to create an {@link AccountIdentity}.
	 */
	public static final class Builder {

		private @Nullable EntityId id;
		private @Nullable AccountIdentityStatus status;
		private @Nullable String email;
		private @Nullable String displayName;
		private @Nullable Avatar avatar;
		private @Nullable Collection<? extends GrantedAuthority> authorities;

		private Builder() {
		}

		/**
		 * Specify the {@link EntityId} of the user account behind this {@link AccountIdentity}.
		 *
		 * @param id account entity identifier, can't be {@literal null}
		 * @return account identity builder
		 */
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the current {@link AccountIdentityStatus} of this {@link AccountIdentity}.
		 *
		 * @param status account identity status, can't be {@literal null}
		 * @return account identity builder
		 */
		public Builder status(AccountIdentityStatus status) {
			this.status = status;
			return this;
		}

		/**
		 * Specify the e-mail address of this {@link AccountIdentity}.
		 *
		 * @param email e-mail address, can't be {@literal null}
		 * @return account identity builder
		 */
		public Builder email(String email) {
			this.email = email;
			return this;
		}

		/**
		 * Specify the display name of this {@link AccountIdentity}.
		 *
		 * @param displayName display name, can't be {@literal null}
		 * @return account identity builder
		 */
		public Builder displayName(String displayName) {
			this.displayName = displayName;
			return this;
		}

		/**
		 * Specify the {@link Avatar} of this {@link AccountIdentity}.
		 *
		 * @param avatar account avatar, can't be {@literal null}
		 * @return account identity builder
		 */
		public Builder avatar(Avatar avatar) {
			this.avatar = avatar;
			return this;
		}

		/**
		 * Specify the {@link GrantedAuthority authorities} granted to this {@link AccountIdentity}.
		 *
		 * @param authorities granted authorities, can't be {@literal null}
		 * @return account identity builder
		 */
		public Builder authorities(Collection<? extends GrantedAuthority> authorities) {
			this.authorities = authorities;
			return this;
		}

		/**
		 * Creates a new instance of the {@link AccountIdentity} using the values defined in the builder.
		 *
		 * @return account identity instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		public AccountIdentity build() {
			Assert.notNull(id, "Account entity identifier can not be null");
			Assert.notNull(status, "Account identity status can not be null");
			Assert.hasText(email, "Account e-mail address can not be blank");
			Assert.hasText(displayName, "Account display name can not be blank");
			Assert.notNull(avatar, "Account avatar can not be null");
			Assert.notNull(authorities, "Account granted authorities can not be null");
			return new AccountIdentity(id, status, email, displayName, avatar, authorities);
		}

	}

}
