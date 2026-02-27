package com.konfigyr.test;

import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.PrincipalType;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * The assertion instance for all {@link AuthenticatedPrincipal} types.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public final class AuthenticatedPrincipalAssert extends AbstractObjectAssert<AuthenticatedPrincipalAssert, AuthenticatedPrincipal> {

	/**
	 * Creates a new {@link AuthenticatedPrincipalAssert} with the given principal value to check.
	 *
	 * @param principal the actual principal to verify
	 * @return Authenticated principal assert
	 */
	@NonNull
	public static AuthenticatedPrincipalAssert assertThat(AuthenticatedPrincipal principal) {
		return new AuthenticatedPrincipalAssert(principal);
	}

	/**
	 * Create an {@link InstanceOfAssertFactory} that can be used to create {@link AuthenticatedPrincipalAssert} for
	 * an asserted object.
	 *
	 * @return Authenticated principal assert factory
	 */
	@NonNull
	public static InstanceOfAssertFactory<? extends AuthenticatedPrincipal, AuthenticatedPrincipalAssert> factory() {
		return new InstanceOfAssertFactory<>(AuthenticatedPrincipal.class, AuthenticatedPrincipalAssert::assertThat);
	}

	private AuthenticatedPrincipalAssert(AuthenticatedPrincipal actual) {
		super(actual, AuthenticatedPrincipalAssert.class);
	}

	/**
	 * Checks if the given {@link AuthenticatedPrincipal} has a matching subject value.
	 *
	 * @param value value to be checked
	 * @return the authenticated principal assert object, never {@literal null}
	 */
	@NonNull
	public AuthenticatedPrincipalAssert hasSubject(String value) {
		isNotNull();

		if (!Objects.equals(value, actual.get())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Authenticated Principal should have a subject of '%s' but was '%s'",
					value, actual.get()
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link AuthenticatedPrincipal} is of {@link PrincipalType#USER_ACCOUNT} type.
	 *
	 * @return the authenticated principal assert object, never {@literal null}
	 */
	public AuthenticatedPrincipalAssert isUserAccount() {
		return hasType(PrincipalType.USER_ACCOUNT);
	}

	/**
	 * Checks if the given {@link AuthenticatedPrincipal} is of {@link PrincipalType#OAUTH_CLIENT} type.
	 *
	 * @return the authenticated principal assert object, never {@literal null}
	 */
	public AuthenticatedPrincipalAssert isClient() {
		return hasType(PrincipalType.OAUTH_CLIENT);
	}

	/**
	 * Checks if the given {@link AuthenticatedPrincipal} is of {@link PrincipalType#SYSTEM} type.
	 *
	 * @return the authenticated principal assert object, never {@literal null}
	 */
	public AuthenticatedPrincipalAssert isSystem() {
		return hasType(PrincipalType.SYSTEM);
	}

	/**
	 * Checks if the given {@link AuthenticatedPrincipal} has a matching principal type.
	 *
	 * @param type principal type to be checked
	 * @return the authenticated principal assert object, never {@literal null}
	 */
	@NonNull
	public AuthenticatedPrincipalAssert hasType(PrincipalType type) {
		isNotNull();

		if (!Objects.equals(type, actual.getType())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Authenticated Principal should have a type of '%s' but was '%s'",
					type, actual.getType()
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link AuthenticatedPrincipal} has a matching email value.
	 *
	 * @param value value to be checked
	 * @return the authenticated principal assert object, never {@literal null}
	 */
	@NonNull
	public AuthenticatedPrincipalAssert hasEmail(String value) {
		isNotNull();

		if (actual.getEmail().isEmpty()) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Authenticated Principal should have an email of '%s' but it was empty",
					value
			));
		}

		final String email = actual.getEmail().get();

		if (!Objects.equals(value, email)) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Authenticated Principal should have an email of '%s' but was '%s'",
					value, email
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link AuthenticatedPrincipal} has no email attribute.
	 *
	 * @return the authenticated principal assert object, never {@literal null}
	 */
	@NonNull
	public AuthenticatedPrincipalAssert hasNoEmail() {
		isNotNull();

		if (actual.getEmail().isPresent()) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Authenticated Principal should not have an email but was '%s'",
					actual.getEmail().get()
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link AuthenticatedPrincipal} has a matching display name value.
	 *
	 * @param value value to be checked
	 * @return the authenticated principal assert object, never {@literal null}
	 */
	@NonNull
	public AuthenticatedPrincipalAssert hasDisplayName(String value) {
		isNotNull();

		if (actual.getDisplayName().isEmpty()) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Authenticated Principal should have a display name of '%s' but it was empty",
					value
			));
		}

		final String displayName = actual.getDisplayName().get();

		if (!Objects.equals(value, displayName)) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Authenticated Principal should have a display name of '%s' but was '%s'",
					value, displayName
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link AuthenticatedPrincipal} has no display name attribute.
	 *
	 * @return the authenticated principal assert object, never {@literal null}
	 */
	@NonNull
	public AuthenticatedPrincipalAssert hasNoDisplayName() {
		isNotNull();

		if (actual.getDisplayName().isPresent()) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Authenticated Principal should not have display name but was '%s'",
					actual.getDisplayName().get()
			));
		}

		return myself;
	}

}
