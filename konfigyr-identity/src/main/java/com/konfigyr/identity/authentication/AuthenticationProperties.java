package com.konfigyr.identity.authentication;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for the Konfigyr Identity authentication domain.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties("konfigyr.identity.authentication")
public class AuthenticationProperties {

	@Valid
	@NestedConfigurationProperty
	private RememberMe rememberMe = new RememberMe();

	@Getter
	@Setter
	public static class RememberMe {

		/**
		 * Shared secret included in the remember-me cookie hash and used to pair tokens with their
		 * issuing {@link org.springframework.security.web.authentication.RememberMeServices} instance.
		 * <p>
		 * The value is concatenated into the {@code SHA-256} input alongside the username and expiry
		 * time, so possession of this secret is required to produce or verify any valid cookie token.
		 * <p>
		 * The {@link org.springframework.security.authentication.RememberMeAuthenticationProvider}
		 * additionally checks {@code key.hashCode()} to ensure tokens are only accepted by the
		 * provider that issued them.
		 */
		@NotEmpty
		private String key;

	}

}
