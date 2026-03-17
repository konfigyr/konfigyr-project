package com.konfigyr.security;

import com.konfigyr.security.basic.NamespaceApplicationDetailsService;
import com.konfigyr.security.oauth.AuthenticatedPrincipalAuthenticationToken;
import com.konfigyr.security.oauth.RequestAttributeBearerTokenResolver;
import com.konfigyr.security.provider.ConfigClientAuthenticationProvider;
import com.konfigyr.web.WebExceptionHandler;
import org.jooq.DSLContext;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import tools.jackson.databind.ObjectMapper;

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
	ProblemDetailsAuthenticationExceptionHandler problemDetailsAuthenticationExceptionHandler(
			ObjectMapper mapper, WebExceptionHandler exceptionHandler
	) {
		return new ProblemDetailsAuthenticationExceptionHandler(mapper, exceptionHandler);
	}

	@Bean
	@Order(1)
	SecurityFilterChain konfigyrConfigClientSecurityFilterChain(
			HttpSecurity http, DSLContext dslContext, ObjectProvider<@NonNull PasswordEncoder> passwordEncoder
	) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(new NamespaceApplicationDetailsService(dslContext));
		provider.setPasswordEncoder(passwordEncoder.getIfAvailable(PasswordEncoders::get));

		return http
				.securityMatcher("/configs/**")
				.authorizeHttpRequests(requests -> requests
						.anyRequest().hasAuthority(OAuthScope.READ_PROFILES.getAuthority())
				)
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.ignoringRequestMatchers("/configs/**"))
				.logout(AbstractHttpConfigurer::disable)
				.httpBasic(Customizer.withDefaults())
				.formLogin(AbstractHttpConfigurer::disable)
				.anonymous(AbstractHttpConfigurer::disable)
				.rememberMe(AbstractHttpConfigurer::disable)
				.sessionManagement(sessions -> sessions
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				.authenticationProvider(provider)
				.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain konfigyrSecurityFilterChain(HttpSecurity http, ProblemDetailsAuthenticationExceptionHandler exceptionHandler) {
		final BearerTokenResolver bearerTokenResolver = new RequestAttributeBearerTokenResolver();

		return http
				.authorizeHttpRequests(requests -> requests
						.anyRequest()
						.authenticated()
				)
				.cors(Customizer.withDefaults())
				.csrf(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.anonymous(AbstractHttpConfigurer::disable)
				.rememberMe(AbstractHttpConfigurer::disable)
				.oauth2ResourceServer(server -> server
						.jwt(jwt -> jwt
								.jwtAuthenticationConverter(AuthenticatedPrincipalAuthenticationToken::of)
						)
						.bearerTokenResolver(bearerTokenResolver)
						.authenticationEntryPoint(exceptionHandler)
						.accessDeniedHandler(exceptionHandler)
				)
				.securityContext(context -> context
						.securityContextRepository(securityContextRepository())
				)
				.sessionManagement(sessions -> sessions
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				.exceptionHandling(errors -> errors
						.defaultAuthenticationEntryPointFor(exceptionHandler, AnyRequestMatcher.INSTANCE)
						.defaultAccessDeniedHandlerFor(exceptionHandler, AnyRequestMatcher.INSTANCE)
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
