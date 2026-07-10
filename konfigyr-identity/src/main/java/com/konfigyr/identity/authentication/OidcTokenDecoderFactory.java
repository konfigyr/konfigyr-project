package com.konfigyr.identity.authentication;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.converter.ClaimTypeConverter;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.util.Assert;
import org.springframework.web.client.RestOperations;

import java.time.Duration;

/**
 * A {@link JwtDecoderFactory} that resolves the JWS algorithms accepted for an OIDC ID Token
 * from the {@link ClientRegistration}'s JWK Set instead of assuming a fixed {@code RS256} algorithm.
 * <p>
 * When a JWK Set URI is present, the accepted algorithms are discovered from the signature keys
 * published at that URI. When it is absent, decoding is delegated to a {@link OidcIdTokenDecoderFactory}
 * configured with the same {@link #clockSkew}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see OidcIdTokenDecoderFactory
 * @see NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder#discoverJwsAlgorithms()
 */
@NullMarked
final class OidcTokenDecoderFactory implements JwtDecoderFactory<ClientRegistration> {

	private final RestOperations restOperations;

	private final Duration clockSkew;

	private final JwtDecoderFactory<ClientRegistration> delegate;

	private final ClaimTypeConverter claimTypeConverter = OidcIdTokenDecoderFactory.createDefaultClaimTypeConverter();

	OidcTokenDecoderFactory(RestOperations restOperations, Duration clockSkew) {
		this.restOperations = restOperations;
		this.clockSkew = clockSkew;
		this.delegate = createDefaultDecoderFactory(clockSkew);
	}

	@Override
	public JwtDecoder createDecoder(ClientRegistration clientRegistration) {
		Assert.notNull(clientRegistration, "clientRegistration cannot be null");

		final String jwkSetUri = clientRegistration.getProviderDetails().getJwkSetUri();

		if (StringUtils.isBlank(jwkSetUri)) {
			return delegate.createDecoder(clientRegistration);
		}

		final NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
				.restOperations(restOperations)
				.discoverJwsAlgorithms()
				.build();

		decoder.setClaimSetConverter(claimTypeConverter);
		decoder.setJwtValidator(createTokenValidator(clientRegistration, clockSkew));
		return decoder;
	}

	private static OAuth2TokenValidator<Jwt> createTokenValidator(ClientRegistration clientRegistration, Duration clockSkew) {
		final OidcIdTokenValidator oidcIdTokenValidator = new OidcIdTokenValidator(clientRegistration);
		oidcIdTokenValidator.setClockSkew(clockSkew);

		return JwtValidators.createDefaultWithValidators(oidcIdTokenValidator, new JwtTimestampValidator(clockSkew));
	}

	private static JwtDecoderFactory<ClientRegistration> createDefaultDecoderFactory(Duration clockSkew) {
		final OidcIdTokenDecoderFactory decoderFactory = new OidcIdTokenDecoderFactory();
		decoderFactory.setJwtValidatorFactory(clientRegistration -> createTokenValidator(clientRegistration, clockSkew));
		return decoderFactory;
	}

}
