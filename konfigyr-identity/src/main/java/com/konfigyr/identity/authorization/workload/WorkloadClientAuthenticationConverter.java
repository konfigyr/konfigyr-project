package com.konfigyr.identity.authorization.workload;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An {@link AuthenticationConverter} for the OAuth 2.0 Token Exchange grant that extracts
 * the {@code client_id} from the request body and produces an unauthenticated
 * {@link OAuth2ClientAuthenticationToken} with {@link org.springframework.security.oauth2.core.ClientAuthenticationMethod#NONE}.
 * Returns {@code null} for all other grant types.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
class WorkloadClientAuthenticationConverter implements AuthenticationConverter {

	@Override
	public @Nullable Authentication convert(HttpServletRequest request) {
		final String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);

		if (AuthorizationGrantType.TOKEN_EXCHANGE.getValue().equals(grantType)) {
			final String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);

			if (!StringUtils.hasText(clientId) || request.getParameterValues(OAuth2ParameterNames.CLIENT_ID).length != 1) {
				throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
			}

			final Map<String, Object> additionalParameters = new HashMap<>();

			for (Iterator<String> it = request.getParameterNames().asIterator(); it.hasNext(); ) {
				final String name = it.next();

				if (OAuth2ParameterNames.CLIENT_ID.equals(name)) {
					continue;
				}

				final String[] values = request.getParameterValues(name);
				additionalParameters.put(name, values.length == 1 ? values[0] : values);
			}

			return new OAuth2ClientAuthenticationToken(clientId, ClientAuthenticationMethod.NONE, null,
					additionalParameters);
		}

		return null;
	}

}
