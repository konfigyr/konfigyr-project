package com.konfigyr.test.assertions;

import org.assertj.core.api.InstanceOfAssertFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.OAuth2Token;

/**
 * The assertion instance for all {@link OAuth2Token} types.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class OAuth2TokenAssert extends AbstractOAuth2TokenAssert<OAuth2TokenAssert, OAuth2Token> {

	/**
	 * Creates a new {@link OAuth2TokenAssert} with the given token value to check.
	 *
	 * @param token the actual token to verify
	 * @param <T> OAuth 2.0 token type
	 * @return OAuth token assert
	 */
	@NonNull
	public static <T extends OAuth2Token> OAuth2TokenAssert assertThat(T token) {
		return new OAuth2TokenAssert(token);
	}

	/**
	 * Create an {@link InstanceOfAssertFactory} that can be used to create {@link OAuth2TokenAssert} for
	 * an asserted object.
	 *
	 * @return OAuth token assert factory
	 */
	@NonNull
	public static InstanceOfAssertFactory<? extends OAuth2Token, OAuth2TokenAssert> factory() {
		return new InstanceOfAssertFactory<>(OAuth2Token.class, OAuth2TokenAssert::assertThat);
	}

	/**
	 * Creates a new {@link OAuth2TokenAssert}.
	 *
	 * @param token the actual token to verify
	 */
	protected OAuth2TokenAssert(OAuth2Token token) {
		super(token, OAuth2TokenAssert.class);
	}

}
