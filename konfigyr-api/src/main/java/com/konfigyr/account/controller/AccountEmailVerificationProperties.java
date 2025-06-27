package com.konfigyr.account.controller;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Configuration properties used to customize the {@link org.springframework.security.oauth2.jwt.JwtEncoder}
 * and {@link org.springframework.security.oauth2.jwt.JwtDecoder} instances used by the {@link AccountEmailVerificationService}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "konfigyr.account.email-verification")
class AccountEmailVerificationProperties {

	/**
	 * The secret key used to sign and verify the issued JWT token.
	 */
	@NotNull
	private Resource secret;

	/**
	 * Signature algorithm to be used to create a JSON Web Signature (JWS). Defaults to <code>HS256</code>.
	 */
	@NotNull
	private MacAlgorithm algorithm = MacAlgorithm.HS256;

	/**
	 * Defines the expiration duration for the issued email verification code. The expiry is added to the
	 * verification JSON Web Token (JWT) as <code>exp</code> claim. Defaults to <code>5m</code>.
	 */
	@NotNull
	@DurationUnit(ChronoUnit.MINUTES)
	private Duration expiration = Duration.ofMinutes(5);

	/**
	 * Defines the number of digits that the one-time email verification code would have.
	 * <p>
	 * Keep this number between <code>6</code> and <code>8</code>, defaults to <code>6</code>.
	 */
	@Range(min = 6, max = 8)
	private int digits = 6;

}
