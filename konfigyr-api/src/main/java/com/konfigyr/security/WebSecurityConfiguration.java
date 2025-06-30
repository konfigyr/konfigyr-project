package com.konfigyr.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.konfigyr.security.oauth.RequestAttributeBearerTokenResolver;
import com.konfigyr.web.WebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import java.util.Collection;

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
	SecurityFilterChain konfigyrSecurityFilterChain(HttpSecurity http, ProblemDetailsAuthenticationExceptionHandler exceptionHandler) throws Exception {
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
								.jwtAuthenticationConverter(jwtAuthenticationConverter())
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

	/**
	 * Creates a customized {@link JwtAuthenticationConverter} that would use the {@link OAuthScopes}
	 * to convert the {@link GrantedAuthority granted authorities} that would be assigned to the
	 * {@link org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken}.
	 *
	 * @return the customized JWT authentication token
	 */
	@SuppressWarnings("unchecked")
	private static JwtAuthenticationConverter jwtAuthenticationConverter() {
		final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setPrincipalClaimName(JwtClaimNames.SUB);
		converter.setJwtGrantedAuthoritiesConverter(
				jwt -> (Collection<GrantedAuthority>) OAuthScopes.from(jwt).toAuthorities());
		return converter;
	}

}
