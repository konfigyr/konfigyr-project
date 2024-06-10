package com.konfigyr.security.provision;

import com.konfigyr.security.PrincipalService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Spring {@link org.springframework.security.config.annotation.SecurityConfigurer} that would register the
 * following Provisioning Filters:
 * <ul>
 *     <li>
 *         <strong>{@link ProvisioningRedirectFilter}</strong>
 *         Redirects the user to the provisioning page where they would be able to create their
 *         accounts and their first namespace.
 *     </li>
 *     <li>
 *         <strong>{@link ProvisioningAuthenticationFilter}</strong>
 *         Filter that is invoked when the provisioning was successfully performed that would
 *         setup the {@link org.springframework.security.core.Authentication} for the provisioned
 *         user account/
 *     </li>
 * </ul>
 *
 * @param <H> HTTP security builder type
 * @author Vladimir Spasic
 **/
public class ProvisioningConfigurer<H extends HttpSecurityBuilder<H>>
		extends AbstractHttpConfigurer<ProvisioningConfigurer<H>, H> {

	private PrincipalService principalService;

	private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	private final List<RequestMatcher> ignoredRequestMatchers = new ArrayList<>();
	private final ProvisioningRedirectFilter redirectFilter = new ProvisioningRedirectFilter();
	private final ProvisioningAuthenticationFilter authenticationFilter = new ProvisioningAuthenticationFilter();

	/**
	 * Configures a custom provisioning URL where the {@link ProvisioningRedirectFilter} would
	 * redirect the user when a {@link com.konfigyr.security.provisioning.ProvisioningRequiredException}
	 * is detected in the request.
	 *
	 * @param provisioningUrl custom provisioning page redirect URL
	 * @return the provisioning configurer
	 * @throws IllegalArgumentException when redirect URL is {@code null} or blank
	 */
	public ProvisioningConfigurer<H> provisioningRedirectUrl(String provisioningUrl) {
		this.redirectFilter.setProvisioningUrl(provisioningUrl);
		return this;
	}

	/**
	 * Allows specifying {@link RequestMatcher request matchers} that should not be redirected
	 * to the provisioning page by the {@link ProvisioningRedirectFilter}.
	 *
	 * @param requestMatchers request matchers
	 * @return the provisioning configurer
	 * @throws IllegalArgumentException when one of the request matchers is {@code null}
	 */
	public ProvisioningConfigurer<H> ignoringRequestMatchers(RequestMatcher... requestMatchers) {
		Assert.noNullElements(requestMatchers, "One of the request matchers was null");
		Collections.addAll(ignoredRequestMatchers, requestMatchers);
		return this;
	}

	/**
	 * Allows specifying {@link RequestMatcher request matchers} that should not be redirected
	 * to the provisioning page by the {@link ProvisioningRedirectFilter}.
	 *
	 * @param patterns request matcher patterns
	 * @return the provisioning configurer
	 * @throws IllegalArgumentException when one of the request matchers is {@code null}
	 */
	public ProvisioningConfigurer<H> ignoringRequestMatchers(String... patterns) {
		Assert.noNullElements(patterns, "One of the request matcher patterns was null");
		for (final String pattern : patterns) {
			ignoredRequestMatchers.add(AntPathRequestMatcher.antMatcher(pattern));
		}
		return this;
	}

	/**
	 * Configures a custom provisioning processing URL where the authentication filter would
	 * extract the {@link com.konfigyr.account.AccountRegistration} request attribute.
	 *
	 * @param provisioningProcessingUrl custom provisioning page processing URL
	 * @return the provisioning configurer
	 * @throws IllegalArgumentException when processing URL is {@code null} or blank
	 */
	public ProvisioningConfigurer<H> provisioningProcessingUrl(String provisioningProcessingUrl) {
		this.authenticationFilter.setFilterProcessesUrl(provisioningProcessingUrl);
		return this;
	}

	/**
	 * Specify the {@link PrincipalService} that would be used to load the authenticated user.
	 *
	 * @param principalService principal service to be used
	 * @return the provisioning configurer
	 * @throws IllegalArgumentException when principal service is {@code null}
	 */
	public ProvisioningConfigurer<H> principalService(PrincipalService principalService) {
		Assert.notNull(principalService, "Principal service can not be null");
		this.principalService = principalService;
		return this;
	}

	/**
	 * Specifies the {@link AuthenticationFailureHandler} to use when authentication fails.
	 *
	 * @param authenticationFailureHandler custom authentication failure handler to be used
	 * @return the provisioning configurer
	 */
	public ProvisioningConfigurer<H> failureHandler(AuthenticationFailureHandler authenticationFailureHandler) {
		this.authenticationFilter.setAuthenticationFailureHandler(authenticationFailureHandler);
		return this;
	}

	/**
	 * Sets the {@link RedirectStrategy} instance to use in {@link ProvisioningRedirectFilter}.
	 *
	 * @param redirectStrategy custom redirect strategy
	 * @return the provisioning configurer
	 * @throws IllegalArgumentException when redirect strategy is {@code null}
	 */
	public ProvisioningConfigurer<H> redirectStrategy(RedirectStrategy redirectStrategy) {
		this.redirectStrategy = redirectStrategy;
		return this;
	}

	@Override
	public void configure(H http) {
		final ProvisioningRedirectFilter provisioningRedirectFilter = getProvisioningRedirectFilter();
		final ProvisioningAuthenticationFilter provisioningProcessingFilter = getProvisioningAuthenticationFilter(http);

		http.authenticationProvider(createProvisioningAuthenticationProvider());

		http.addFilterBefore(provisioningRedirectFilter, OAuth2AuthorizationRequestRedirectFilter.class);
		http.addFilterBefore(provisioningProcessingFilter, ProvisioningRedirectFilter.class);
	}

	protected ProvisioningRedirectFilter getProvisioningRedirectFilter() {
		redirectFilter.setRedirectStrategy(redirectStrategy);

		if (!ignoredRequestMatchers.isEmpty()) {
			redirectFilter.setIgnoringRequestMatcher(new OrRequestMatcher(ignoredRequestMatchers));
		}

		return postProcess(redirectFilter);
	}

	protected ProvisioningAuthenticationFilter getProvisioningAuthenticationFilter(H http) {
		authenticationFilter.setAuthenticationSuccessHandler(createAuthenticationSuccessHandler(http));
		authenticationFilter.setAuthenticationManager(http.getSharedObject(AuthenticationManager.class));
		authenticationFilter.setSecurityContextHolderStrategy(getSecurityContextHolderStrategy());

		final SessionAuthenticationStrategy sessionAuthenticationStrategy = http
				.getSharedObject(SessionAuthenticationStrategy.class);
		if (sessionAuthenticationStrategy != null) {
			authenticationFilter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy);
		}

		final RememberMeServices rememberMeServices = http.getSharedObject(RememberMeServices.class);
		if (rememberMeServices != null) {
			authenticationFilter.setRememberMeServices(rememberMeServices);
		}

		final SecurityContextRepository securityContextRepository = http.getSharedObject(SecurityContextRepository.class);
		if (securityContextRepository != null) {
			authenticationFilter.setSecurityContextRepository(securityContextRepository);
		}

		return postProcess(authenticationFilter);
	}

	protected AuthenticationProvider createProvisioningAuthenticationProvider() {
		Assert.notNull(principalService, "Principal service can not be null");

		final AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> service =
				new UserDetailsByNameServiceWrapper<>(principalService::lookup);

		final PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
		provider.setPreAuthenticatedUserDetailsService(service);
		provider.setThrowExceptionWhenTokenRejected(true);
		return provider;
	}

	private AuthenticationSuccessHandler createAuthenticationSuccessHandler(H http) {
		final SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
		handler.setDefaultTargetUrl("/");
		handler.setAlwaysUseDefaultTargetUrl(true);
		handler.setRedirectStrategy(redirectStrategy);

		final RequestCache requestCache = http.getSharedObject(RequestCache.class);
		if (requestCache != null) {
			handler.setRequestCache(requestCache);
		}

		return handler;
	}

}
