package com.konfigyr.security;

import com.konfigyr.security.provision.ProvisioningConfigurer;
import com.konfigyr.security.provision.ProvisioningRedirectFilter;
import com.konfigyr.security.rememberme.AccountRememberMeServices;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author Vladimir Spasic
 **/
@ComponentScan
@EnableCaching
@EnableWebSecurity
@EnableAutoConfiguration
@Configuration(proxyBeanMethods = false)
public class SecurityTestConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, PrincipalService principalService) throws Exception {
		return http
				.authorizeHttpRequests(requests -> requests
						.requestMatchers(ProvisioningRedirectFilter.DEFAULT_PROVISIONING_URL)
						.permitAll()
				)
				.rememberMe(rememberMe -> rememberMe
						.rememberMeServices(new AccountRememberMeServices(principalService))
				)
				.with(new ProvisioningConfigurer<>(), provision -> provision
						.principalService(principalService)
				)
				.build();
	}

}
