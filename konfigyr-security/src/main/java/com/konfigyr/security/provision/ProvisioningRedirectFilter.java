package com.konfigyr.security.provision;

import com.konfigyr.security.AccountPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security filter that should check if the current {@link Authentication security principal} requires
 * account provisioning, and if so, it would redirect the user to the defined account provisioning page.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class ProvisioningRedirectFilter extends OncePerRequestFilter {

	static final String DEFAULT_PROVISIONING_URL = "/provision";

	private String provisioningUrl = DEFAULT_PROVISIONING_URL;

	private RequestMatcher requestMatcher = AntPathRequestMatcher.antMatcher(DEFAULT_PROVISIONING_URL);

	private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	private SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain
	) throws ServletException, IOException {
		if (requiresProvisioning()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Redirecting to account provisioning page as account is not active: " + provisioningUrl);
			}

			redirectStrategy.sendRedirect(request, response, provisioningUrl);
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		return requestMatcher.matches(request);
	}

	/**
	 * Sets the Account Provisioning Endpoint redirect URI.
	 *
	 * @param provisioningUrl the redirect URI
	 * @throws IllegalArgumentException when redirect IRU is {@literal null} or blank
	 */
	public void setProvisioningUrl(String provisioningUrl) {
		Assert.hasText(provisioningUrl, "Provisioning URL can not be blank");
		this.provisioningUrl = provisioningUrl;
		this.requestMatcher = AntPathRequestMatcher.antMatcher(provisioningUrl);
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

	/**
	 * Sets the {@link SecurityContextHolderStrategy} to use. The default behaviour is to use the
	 * {@link SecurityContextHolderStrategy} stored in the {@link SecurityContextHolder}.
	 *
	 * @param contextHolderStrategy security context holder strategy to be used
	 * @throws IllegalArgumentException when strategy is {@literal null}
	 */
	public void setContextHolderStrategy(SecurityContextHolderStrategy contextHolderStrategy) {
		Assert.notNull(contextHolderStrategy, "Security context holder strategy can not be null");
		this.contextHolderStrategy = contextHolderStrategy;
	}

	private boolean requiresProvisioning() {
		final Authentication authentication = contextHolderStrategy.getContext().getAuthentication();

		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			return false;
		}

		if (ClassUtils.isAssignableValue(AccountPrincipal.class, authentication.getPrincipal())) {
			return false;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Detected an authentication that requires provisioning: " + authentication);
		}

		return true;
	}

}
