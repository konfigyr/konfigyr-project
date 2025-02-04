package com.konfigyr.security;

import com.konfigyr.security.csrf.CsrfRequestMatcher;
import com.konfigyr.security.oauth.RequestAttributeBearerTokenResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

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
	SecurityFilterChain konfigyrSecurityFilterChain(HttpSecurity http) throws Exception {
		final BearerTokenResolver bearerTokenResolver = new RequestAttributeBearerTokenResolver();

		return http
				.authorizeHttpRequests(requests -> requests
						.anyRequest()
						.authenticated()
				)
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf
						.requireCsrfProtectionMatcher(new CsrfRequestMatcher(bearerTokenResolver))
				)
				.logout(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.anonymous(AbstractHttpConfigurer::disable)
				.rememberMe(AbstractHttpConfigurer::disable)
				.oauth2ResourceServer(server -> server
						.jwt(Customizer.withDefaults())
						.bearerTokenResolver(bearerTokenResolver)
				)
				.securityContext(context -> context
						.securityContextRepository(securityContextRepository())
				)
				.sessionManagement(sessions -> sessions
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				.exceptionHandling(errors -> errors
						.defaultAccessDeniedHandlerFor(new BearerTokenAccessDeniedHandler(), AnyRequestMatcher.INSTANCE)
						.defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), AnyRequestMatcher.INSTANCE)
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
