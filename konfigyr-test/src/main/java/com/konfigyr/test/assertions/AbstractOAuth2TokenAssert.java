package com.konfigyr.test.assertions;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.TemporalOffset;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.core.OAuth2Token;

import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Objects;

/**
 * Abstract assert class that should be used by implementations of {@link OAuth2Token}.
 *
 * @param <A> the actual assertion type
 * @param <T> the OAuth 2.0 token type
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public abstract class AbstractOAuth2TokenAssert<A extends AbstractOAuth2TokenAssert<A, T>, T extends OAuth2Token>
		extends AbstractObjectAssert<A, T> {

	protected AbstractOAuth2TokenAssert(T token, Class<? extends A> type) {
		super(token, type);
	}

	/**
	 * Checks if the given {@link OAuth2Token} has a matching value.
	 *
	 * @param value value to be checked
	 * @return the token assert object, never {@literal null}
	 */
	@NonNull
	public A hasValue(String value) {
		isNotNull();

		if (!Objects.equals(value, actual.getTokenValue())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that OAuth 2.0 Token should have a value of %s but was %s",
					value, actual.getTokenValue()
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link OAuth2Token} was issued at the given instant.
	 *
	 * @param instant issued at instant to be checked
	 * @return the token assert object, never {@literal null}
	 */
	@NonNull
	public A issuedAt(Instant instant) {
		isNotNull();

		return satisfies(it -> Assertions.assertThat(it.getIssuedAt())
				.describedAs("Expected that OAuth 2.0 Token should be issued at %s but was %s", instant, it.getIssuedAt())
				.isEqualTo(instant));
	}

	/**
	 * Checks if the given {@link OAuth2Token} was issued at the given instant with a given offset.
	 *
	 * @param instant issued at instant to be checked
	 * @param offset the offset used for comparison
	 * @return the token assert object, never {@literal null}
	 */
	@NonNull
	public A issuedAt(Instant instant, TemporalOffset<Temporal> offset) {
		isNotNull();

		return satisfies(it -> Assertions.assertThat(it.getIssuedAt())
				.describedAs("Expected that OAuth 2.0 Token should be issued at %s but was %s", instant, it.getIssuedAt())
				.isCloseTo(instant, offset));
	}

	/**
	 * Checks if the given {@link OAuth2Token} has a matching expiration instant.
	 *
	 * @param instant expiration instant to be checked
	 * @return the token assert object, never {@literal null}
	 */
	@NonNull
	public A expiresAt(Instant instant) {
		isNotNull();

		return satisfies(it -> Assertions.assertThat(it.getExpiresAt())
				.describedAs("Expected that OAuth 2.0 Token should expire at %s but was %s", instant, it.getExpiresAt())
				.isEqualTo(instant));
	}

	/**
	 * Checks if the given {@link OAuth2Token} has a matching expiration instant with a given offset.
	 *
	 * @param instant expiration instant to be checked
	 * @param offset the offset used for comparison
	 * @return the token assert object, never {@literal null}
	 */
	@NonNull
	public A expiresAt(Instant instant, TemporalOffset<Temporal> offset) {
		isNotNull();

		return satisfies(it -> Assertions.assertThat(it.getExpiresAt())
				.describedAs("Expected that OAuth 2.0 Token should expire at %s but was %s", instant, it.getExpiresAt())
				.isCloseTo(instant, offset));
	}
}
