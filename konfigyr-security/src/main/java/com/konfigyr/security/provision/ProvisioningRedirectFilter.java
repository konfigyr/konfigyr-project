package com.konfigyr.security.provision;

import com.konfigyr.security.provisioning.ProvisioningRequiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.lang.NonNull;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security filter that should check if the current {@link HttpSession} contains the
 * {@link ProvisioningRequiredException} and redirect the user to the provisioning page.
 * <p>
 * This exception should be put in the {@link HttpSession} by the configured
 * {@link org.springframework.security.web.authentication.AuthenticationFailureHandler} when
 * {@link com.konfigyr.security.PrincipalService} fails to find an account for the resolved
 * OAuth user.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class ProvisioningRedirectFilter extends OncePerRequestFilter {

	/**
	 * The default provisioning page URL: <code>/provision</code>. This filter would redirect the user
	 * to this endpoint if {@link ProvisioningRequiredException} is present in the {@link HttpSession}.
	 */
	public static final String DEFAULT_PROVISIONING_URL = "/provision";

	private String provisioningUrl = DEFAULT_PROVISIONING_URL;

	private RequestMatcher ignoringRequestMatcher = AntPathRequestMatcher.antMatcher(DEFAULT_PROVISIONING_URL);

	private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain
	) throws ServletException, IOException {
		if (requiresProvisioning(request)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Redirecting to account provisioning page with URL: " + provisioningUrl);
			}

			redirectStrategy.sendRedirect(request, response, provisioningUrl);
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		return ignoringRequestMatcher.matches(request);
	}

	/**
	 * Sets the Account Provisioning Endpoint redirect URL.
	 *
	 * @param provisioningUrl the redirect URL
	 * @throws IllegalArgumentException when redirect URL is {@literal null} or blank
	 */
	public void setProvisioningUrl(String provisioningUrl) {
		Assert.hasText(provisioningUrl, "Provisioning URL can not be blank");
		this.provisioningUrl = provisioningUrl;
		this.ignoringRequestMatcher = new OrRequestMatcher(
				AntPathRequestMatcher.antMatcher(provisioningUrl), ignoringRequestMatcher);
	}

	/**
	 * Sets the {@link RequestMatcher} that would be used to check if this should skip processing
	 * this HTTP request or not.
	 *
	 * @param ignoringRequestMatcher the request matcher to ignore requests by this filter
	 * @throws IllegalArgumentException when request matcher is {@literal null}
	 */
	public void setIgnoringRequestMatcher(RequestMatcher ignoringRequestMatcher) {
		Assert.notNull(ignoringRequestMatcher, "Ignoring request matcher can not be null");

		this.ignoringRequestMatcher = new OrRequestMatcher(
				AntPathRequestMatcher.antMatcher(provisioningUrl), ignoringRequestMatcher);
	}

	/**
	 * Sets the {@link RedirectStrategy} for Account Provisioning Endpoint redirect URI.
	 *
	 * @param redirectStrategy the redirect strategy to be used
	 * @throws IllegalArgumentException when strategy is {@literal null}
	 */
	public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
		Assert.notNull(redirectStrategy, "Redirect strategy can not be null");
		this.redirectStrategy = redirectStrategy;
	}

	private boolean requiresProvisioning(@NonNull HttpServletRequest request) {
		final HttpSession session = request.getSession(false);

		if (session == null) {
			return false;
		}

		final Object exception = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

		if (exception instanceof ProvisioningRequiredException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found provisioning required exception in session with hints: " + ex.getHints());
			}

			return true;
		}

		return false;
	}

}
