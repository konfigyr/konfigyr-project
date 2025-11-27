package com.konfigyr.identity.authorization.jwk;

import com.konfigyr.crypto.KeysetStore;
import com.konfigyr.identity.KonfigyrIdentityKeysets;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import static com.konfigyr.identity.authorization.jwk.KeysetSourceTest.setupKeySource;
import static com.konfigyr.identity.authorization.jwk.KeysetSourceTest.setupStore;

import static org.assertj.core.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KeysetSourceAcceptanceTest {

	KeysetStore store;
	KeysetSource source;

	NimbusJwtEncoder encoder;
	NimbusJwtDecoder decoder;

	@BeforeEach
	void setup() {
		store = setupStore();
		source = setupKeySource(store);
		encoder = createEncoder(source);
		decoder = createDecoder(source);

		source.afterPropertiesSet();
	}

	@Test
	@DisplayName("should encode and decode token with a single JWK in keyset")
	void shouldVerifyToken() {
		final var parameters = createParameters();

		final var jwt = encoder.encode(parameters);

		assertThat(jwt.getTokenValue())
				.isNotNull();

		assertThat(decoder.decode(jwt.getTokenValue()))
				.isNotNull()
				.isEqualTo(jwt);
	}

	@Test
	@DisplayName("should encode JWK with key that would be rotated before decoding")
	void shouldVerifyTokenWithPreviousKey() {
		final var parameters = createParameters();

		final var jwt = encoder.encode(parameters);

		assertThat(jwt.getTokenValue())
				.isNotNull();

		store.write(store.read(KonfigyrIdentityKeysets.WEB_KEYS.getName()).rotate());
		source.reload();

		assertThat(decoder.decode(jwt.getTokenValue()))
				.isNotNull()
				.isEqualTo(jwt);
	}

	static JwtEncoderParameters createParameters() {
		return JwtEncoderParameters.from(
				JwsHeader.with(SignatureAlgorithm.RS256).build(),
				JwtClaimsSet.builder().subject("test-subject").build()
		);
	}

	static NimbusJwtEncoder createEncoder(KeysetSource source) {
		final NimbusJwtEncoder encoder = new NimbusJwtEncoder(source);
		encoder.setJwkSelector(SigningJwkSelector.getInstance()::select);

		return encoder;
	}

	static NimbusJwtDecoder createDecoder(KeysetSource source) {
		return NimbusJwtDecoder.withJwkSource(source)
				.jwsAlgorithm(SignatureAlgorithm.RS256)
				.build();
	}
}
