package com.konfigyr.identity.configuration;

import com.konfigyr.identity.KonfigyrIdentityRequestMatchers;
import com.konfigyr.identity.authentication.AccountIdentityService;
import com.konfigyr.identity.authentication.rememberme.AccountRememberMeServices;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@EnableWebSecurity
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

	private final AccountIdentityService accountIdentityService;

	@Bean
	@Order(1)
	SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
		final OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
				OAuth2AuthorizationServerConfigurer.authorizationServer();

		return http
				.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
				.with(authorizationServerConfigurer, (authorizationServer) ->
						authorizationServer
								// Enable OpenID Connect 1.0
								.oidc(Customizer.withDefaults())
								// Specify a custom OAuth 2.0 client consent page
								.authorizationEndpoint(endpoint -> endpoint
										.consentPage(KonfigyrIdentityRequestMatchers.CONSENTS_PAGE))
				)
				.authorizeHttpRequests((authorize) -> authorize
						.anyRequest().authenticated()
				)
				// Redirect to the login page when not authenticated from the authorization endpoint
				.exceptionHandling((exceptions) -> exceptions
						.defaultAuthenticationEntryPointFor(
								new LoginUrlAuthenticationEntryPoint(KonfigyrIdentityRequestMatchers.LOGIN_PAGE),
								new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
						)
				).build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain konfigyrSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.securityMatchers(requests -> requests
						.requestMatchers(new NegatedRequestMatcher(
								PathRequest.toStaticResources().atCommonLocations()
						))
				)
				.authorizeHttpRequests((authorize) -> authorize
						.requestMatchers(KonfigyrIdentityRequestMatchers.LOGIN_PAGE).permitAll()
						.anyRequest().authenticated()
				)
				.anonymous(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.rememberMe(remember -> remember
						.key(AccountRememberMeServices.KEY)
						.rememberMeServices(new AccountRememberMeServices(accountIdentityService::get))
				)
				.oauth2Login(login -> login
						.loginPage(KonfigyrIdentityRequestMatchers.LOGIN_PAGE)
				)
				.securityContext(context -> context
						.securityContextRepository(securityContextRepository())
				)
				.sessionManagement(sessions -> sessions
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
						.sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::changeSessionId)
				)
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
