package com.konfigyr.security;

import com.konfigyr.security.authentication.AuthenticationFailureHandlerBuilder;
import com.konfigyr.security.provision.ProvisioningConfigurer;
import com.konfigyr.security.provision.ProvisioningRequiredException;
import com.konfigyr.security.rememberme.AccountRememberMeServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

/**
 * Spring web security configuration class that would register the Spring Security OAuth2 Login flow.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Configuration
@EnableMethodSecurity
public class WebSecurityConfiguration {

	@Bean
	SecurityFilterChain konfigyrSecurityFilterChain(HttpSecurity http, PrincipalService detailsService) throws Exception {
		final AuthenticationFailureHandler failureHandler = authenticationFailureHandler();

		return http
				.securityMatcher(
						new NegatedRequestMatcher(new OrRequestMatcher(
								// do not apply for static assets
								SecurityRequestMatchers.STATIC_ASSETS,
								// do not apply for error page
								AntPathRequestMatcher.antMatcher(SecurityRequestMatchers.ERROR_PAGE)
						))
				)
				.authorizeHttpRequests(requests -> requests
						.requestMatchers(
								SecurityRequestMatchers.OAUTH,
								SecurityRequestMatchers.OAUTH_LOGIN
						)
						.permitAll()
						.requestMatchers(
								"/",
								"/namespace/**",
								"/namespaces/check-name",
								SecurityRequestMatchers.PROVISIONING_PAGE
						)
						.permitAll()
						.anyRequest()
						.authenticated()
				)
				.cors(Customizer.withDefaults())
				.oauth2Login(oauth -> oauth
						.loginPage(SecurityRequestMatchers.LOGIN_PAGE)
						.failureHandler(failureHandler)
				)
				.rememberMe(remember -> remember
						.key(AccountRememberMeServices.KEY)
						.rememberMeServices(new AccountRememberMeServices(detailsService))
				)
				.logout(Customizer.withDefaults())
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.anonymous(AbstractHttpConfigurer::disable)
				.exceptionHandling(exceptions -> exceptions
						.defaultAuthenticationEntryPointFor(loginAuthenticationEntryPoint(), AnyRequestMatcher.INSTANCE)
				)
				.securityContext(context -> context
						.securityContextRepository(securityContextRepository())
				)
				.sessionManagement(sessions -> sessions
						.sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
						.sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::changeSessionId)
				)
				.with(new ProvisioningConfigurer<>(), provisioning -> provisioning
						.ignoringRequestMatchers("/namespaces/check-name")
						.principalService(detailsService)
						.failureHandler(failureHandler)
				)
				.build();
	}

	private static AuthenticationEntryPoint loginAuthenticationEntryPoint() {
		final var entryPoint = new LoginUrlAuthenticationEntryPoint(SecurityRequestMatchers.LOGIN_PAGE);
		entryPoint.setUseForward(false);
		entryPoint.afterPropertiesSet();
		return entryPoint;
	}

	private static AuthenticationFailureHandler authenticationFailureHandler() {
		return new AuthenticationFailureHandlerBuilder(SecurityRequestMatchers.AUTHENTICATION_ERROR_PAGE)
				.register(ProvisioningRequiredException.class, SecurityRequestMatchers.PROVISIONING_PAGE)
				.build();
	}

	/**
	 * Create a new {@link DelegatingSecurityContextRepository} that would only store/read the
	 * {@link org.springframework.security.core.context.SecurityContext} in the request attributes and not
	 * in the session.
	 * <p>
	 * Instead on relying on the session to retrieve the context, the context, and the authentication,
	 * would be resolved by the {@link org.springframework.security.web.authentication.RememberMeServices}.
	 *
	 * @return security context repository
	 */
	private static SecurityContextRepository securityContextRepository() {
		return new DelegatingSecurityContextRepository(
				new RequestAttributeSecurityContextRepository()
		);
	}

}
