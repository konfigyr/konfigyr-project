package com.konfigyr.security;

import com.konfigyr.security.rememberme.AccountRememberMeServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
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
	SecurityFilterChain konfigyrSecurityFilterChain(HttpSecurity http, PrincipalService detailsService) throws Exception {
		return http
				.authorizeHttpRequests(requests -> requests
						.requestMatchers(
								SecurityRequestMatchers.STATIC_ASSETS,
								SecurityRequestMatchers.OAUTH,
								SecurityRequestMatchers.OAUTH_LOGIN
						)
						.permitAll()
						.requestMatchers("/")
						.permitAll()
						.anyRequest()
						.authenticated()
				)
				.cors(Customizer.withDefaults())
				.oauth2Login(oauth -> oauth
						.loginPage(SecurityRequestMatchers.LOGIN_PAGE)
				)
				.rememberMe(remember -> remember
						.rememberMeServices(new AccountRememberMeServices(detailsService))
				)
				.logout(Customizer.withDefaults())
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.anonymous(AbstractHttpConfigurer::disable)
				.exceptionHandling(exceptions -> exceptions
						.defaultAuthenticationEntryPointFor(loginAuthenticationEntryPoint(), AnyRequestMatcher.INSTANCE)
				)
				.build();
	}

	private static AuthenticationEntryPoint loginAuthenticationEntryPoint() {
		final var entryPoint = new LoginUrlAuthenticationEntryPoint(SecurityRequestMatchers.LOGIN_PAGE);
		entryPoint.setUseForward(false);
		entryPoint.afterPropertiesSet();
		return entryPoint;
	}

}
