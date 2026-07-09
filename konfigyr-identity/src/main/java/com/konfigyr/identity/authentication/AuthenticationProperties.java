package com.konfigyr.identity.authentication;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

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

	@Valid
	@NestedConfigurationProperty
	private Oidc oidc = new Oidc();

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

	@Getter
	@Setter
	public static class Oidc {

		/**
		 * The clock skew of the JWT token, used to ensure that the token is not expired.
		 * Defaults to 60 seconds.
		 *
		 * @see OidcTokenDecoderFactory
		 */
		@DurationMin(seconds = 0)
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration jwtClockSkew = Duration.ofSeconds(60);

	}
}
