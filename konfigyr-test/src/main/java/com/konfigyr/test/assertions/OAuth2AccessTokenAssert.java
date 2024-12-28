package com.konfigyr.test.assertions;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * The assertion instance for {@link OAuth2AccessToken} types.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public final class OAuth2AccessTokenAssert extends AbstractOAuth2TokenAssert<OAuth2AccessTokenAssert, OAuth2AccessToken> {

	/**
	 * Creates a new {@link OAuth2AccessTokenAssert} with the given token value to check.
	 *
	 * @param token the actual token to verify
	 * @return OAuth Access Token assert
	 */
	@NonNull
	public static OAuth2AccessTokenAssert assertThat(OAuth2AccessToken token) {
		return new OAuth2AccessTokenAssert(token);
	}

	/**
	 * Create an {@link InstanceOfAssertFactory} that can be used to create {@link OAuth2AccessTokenAssert} for
	 * an asserted object.
	 *
	 * @return OAuth Access Token assert factory
	 */
	@NonNull
	public static InstanceOfAssertFactory<OAuth2AccessToken, OAuth2AccessTokenAssert> factory() {
		return new InstanceOfAssertFactory<>(OAuth2AccessToken.class, OAuth2AccessTokenAssert::new);
	}

	private OAuth2AccessTokenAssert(OAuth2AccessToken token) {
		super(token, OAuth2AccessTokenAssert.class);
	}

	/**
	 * Checks if the given {@link OAuth2AccessToken} is a {@link OAuth2AccessToken.TokenType#BEARER} token type.
	 *
	 * @return the token assert object, never {@literal null}
	 */
	@NonNull
	public OAuth2AccessTokenAssert isBearerToken() {
		return hasTokenType(OAuth2AccessToken.TokenType.BEARER);
	}

	/**
	 * Checks if the given {@link OAuth2AccessToken} is of given type.
	 *
	 * @param type token type to be checked
	 * @return the token assert object, never {@literal null}
	 */
	@NonNull
	public OAuth2AccessTokenAssert hasTokenType(OAuth2AccessToken.TokenType type) {
		isNotNull();

		if (!Objects.equals(type, actual.getTokenType())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that OAuth 2.0 Access Token should have a type of \"%s\" but was \"%s\"",
					type == null ? null : type.getValue(), actual.getTokenType().getValue()
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link OAuth2AccessToken} contains any of the following scopes.
	 *
	 * @param scopes scopes to be checked
	 * @return the token assert object, never {@literal null}
	 */
	@NonNull
	public OAuth2AccessTokenAssert containsScopes(String... scopes) {
		return containsScopes(Arrays.asList(scopes));
	}

	/**
	 * Checks if the given {@link OAuth2AccessToken} contains any of the following scopes.
	 *
	 * @param scopes scopes to be checked
	 * @return the token assert object, never {@literal null}
	 */
	@NonNull
	public OAuth2AccessTokenAssert containsScopes(Collection<String> scopes) {
		return isNotNull().satisfies(it -> Assertions.assertThat(it.getScopes())
				.describedAs("Expected that OAuth 2.0 Access Token should contain scopes: %s", scopes)
				.containsAnyElementsOf(scopes)
		);
	}

	/**
	 * Checks if the given {@link OAuth2AccessToken} does not contain any of the following scopes.
	 *
	 * @param scopes scopes to be checked
	 * @return the token assert object, never {@literal null}
	 */
	@NonNull
	public OAuth2AccessTokenAssert doesNotContainScopes(String... scopes) {
		return isNotNull().satisfies(it -> Assertions.assertThat(it.getScopes())
				.describedAs("Expected that OAuth 2.0 Access Token should not contain scopes: %s", Arrays.asList(scopes))
				.doesNotContain(scopes)
		);
	}

	/**
	 * Checks if the given {@link OAuth2AccessToken} contains all the following scopes.
	 *
	 * @param scopes scopes to be checked
	 * @return the token assert object, never {@literal null}
	 */
	@NonNull
	public OAuth2AccessTokenAssert hasScopes(String... scopes) {
		return hasScopes(Arrays.asList(scopes));
	}

	/**
	 * Checks if the given {@link OAuth2AccessToken} contains all the following scopes.
	 *
	 * @param scopes scopes to be checked
	 * @return the token assert object, never {@literal null}
	 */
	@NonNull
	public OAuth2AccessTokenAssert hasScopes(Collection<String> scopes) {
		return isNotNull().satisfies(it -> Assertions.assertThat(it.getScopes())
				.describedAs("Expected that OAuth 2.0 Access Token should contain all scopes: %s", scopes)
				.containsExactlyInAnyOrderElementsOf(scopes)
		);
	}

}
