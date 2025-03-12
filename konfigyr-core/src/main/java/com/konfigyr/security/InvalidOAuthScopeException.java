package com.konfigyr.security;

import lombok.Getter;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

import java.io.Serial;

/**
 * An {@link OAuth2AuthenticationException} that indicates an invalid {@link OAuthScope} value.
 * <p>
 * This exception would use the {@link OAuth2ErrorCodes#INVALID_SCOPE <code>invalid_scope</code>}
 * OAuth 2.0 Error Code when creating a {@link OAuth2Error}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Getter
public class InvalidOAuthScopeException extends OAuth2AuthenticationException {

	@Serial
	private static final long serialVersionUID = 6009980056283676047L;

	@NonNull
	private final String scope;

	/**
	 * Constructs an {@link InvalidOAuthScopeException} for the invalid scope that was detected.
	 *
	 * @param scope the invalid OAuth scope, can not be {@literal null}
	 */
	public InvalidOAuthScopeException(@NonNull String scope) {
		super(new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE, "Invalid OAuth scope of: " + scope, null));
		this.scope = scope;
	}

}
