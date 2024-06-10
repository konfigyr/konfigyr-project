package com.konfigyr.security.provision;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.util.WebUtils;

/**
 * Authentication processing filter that is used to authenticate the provisioned user account.
 * <p>
 * The filter would intercept {@link DispatcherType#FORWARD forwarded HTTP requests} that contain
 * the provisioned user account identifier. The extracted identifier is wrapped within the
 * {@link PreAuthenticatedAuthenticationToken} and evaluated by the
 * {@link org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider}.
 *
 * @author Vladimir Spasic
 **/
public class ProvisioningAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	/**
	 * The default provisioning authentication processing URL: <code>/provision/authenticate</code>.
	 * This filter would attempt to authenticate the provisioned user on this request.
	 */
	public static final String DEFAULT_PROCESSING_URL = "/provision/authenticate";

	protected ProvisioningAuthenticationFilter() {
		super(DEFAULT_PROCESSING_URL);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request,
												HttpServletResponse response) throws AuthenticationException {
		final String account = WebUtils.findParameterValue(request, "account");

		if (account == null) {
			throw new AuthenticationCredentialsNotFoundException("Failed to extract account identifier from the request");
		}

		final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(account, "N/A");

		if (authenticationDetailsSource != null) {
			token.setDetails(authenticationDetailsSource.buildDetails(request));
		}

		return getAuthenticationManager().authenticate(token);
	}

	@Override
	public void setFilterProcessesUrl(String filterProcessesUrl) {
		setRequiresAuthenticationRequestMatcher(createAuthenticationRequestMatcher(filterProcessesUrl));
	}

	private static RequestMatcher createAuthenticationRequestMatcher(String processingUrl) {
		Assert.hasText(processingUrl, "Authentication processing URL can not be blank");

		return new AndRequestMatcher(
				new DispatcherTypeRequestMatcher(DispatcherType.FORWARD),
				AntPathRequestMatcher.antMatcher(processingUrl)
		);
	}
}
