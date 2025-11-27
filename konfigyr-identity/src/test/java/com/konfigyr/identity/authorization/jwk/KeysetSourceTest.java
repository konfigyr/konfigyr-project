package com.konfigyr.identity.authorization.jwk;

import com.konfigyr.crypto.*;
import com.konfigyr.crypto.jose.JoseKeysetFactory;
import com.konfigyr.crypto.tink.TinkKeyEncryptionKey;
import com.konfigyr.identity.KonfigyrIdentityKeysets;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class KeysetSourceTest {

	private static final JWKSelector ALL_SELECTOR = new JWKSelector(new JWKMatcher.Builder().build());

	final SecurityContext context = new SimpleSecurityContext();

	@Mock
	KeysetStore store;

	@Mock(extraInterfaces = JWKSource.class)
	Keyset keyset;

	@Mock
	JWK key;

	@Test
	@DisplayName("should fail to retrieve JWK set on non-initialized source")
	void shouldWaitForInitialization() {
		final var source = setupKeySource(store);

		assertThatThrownBy(() -> source.get(ALL_SELECTOR, context))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWK source has not been initialized");

		verifyNoInteractions(store);
	}

	@Test
	@DisplayName("should initialize JWK Source keyset when not present in the store")
	void shouldCreateKeyset() throws Exception {
		final var store = setupStore();
		final var source = setupKeySource(store);

		assertThatNoException().isThrownBy(source::afterPropertiesSet);

		assertThat(source.get(ALL_SELECTOR, context))
				.hasSize(1);

		verify(store).read(KonfigyrIdentityKeysets.WEB_KEYS.getName());
		verify(store).create(CryptoProperties.PROVIDER_NAME, CryptoProperties.KEK_ID, KonfigyrIdentityKeysets.WEB_KEYS);
	}

	@Test
	@SuppressWarnings("unchecked")
	@DisplayName("should load JWK Source keyset from the store")
	void shouldLoadKeyset() throws Exception {
		final var source = setupKeySource(store);
		doReturn(keyset).when(store).read(KonfigyrIdentityKeysets.WEB_KEYS.getName());
		doReturn(List.of(key)).when((JWKSource<SecurityContext>) keyset).get(ALL_SELECTOR, context);

		assertThatNoException().isThrownBy(source::afterPropertiesSet);

		assertThat(source.get(ALL_SELECTOR, context))
				.containsExactly(key);

		verify((JWKSource<SecurityContext>) keyset).get(ALL_SELECTOR, context);
		verify(store).read(KonfigyrIdentityKeysets.WEB_KEYS.getName());
		verify(store, never()).create(anyString(), anyString(), any());
	}

	@Test
	@DisplayName("should fail to initialize JWK source when using unsupported keyset algorithm")
	void shouldFailToInitializeUnsupportedAlgorithm() {
		final var store = setupStore();
		final var source = new KeysetSource(store, KonfigyrIdentityKeysets.AUTHORIZATIONS);

		assertThatExceptionOfType(CryptoException.UnsupportedKeysetException.class)
				.isThrownBy(source::afterPropertiesSet);
	}

	@Test
	@DisplayName("should fail to reload JWK source for unsupported keyset")
	void shouldFailToReloadUnsupportedKeyset() {
		final var keyset = mock(Keyset.class);
		final var source = setupKeySource(store);

		doReturn(keyset).when(store).read(KonfigyrIdentityKeysets.WEB_KEYS.getName());

		assertThatIllegalStateException()
				.isThrownBy(source::reload)
				.withMessageContaining("Keyset store loaded a keyset that is not a JWK source")
				.withNoCause();
	}

	static KeysetSource setupKeySource(KeysetStore store) {
		return new KeysetSource(store, KonfigyrIdentityKeysets.WEB_KEYS);
	}

	static KeysetStore setupStore() {
		return spy(
				KeysetStore.builder()
						.factories(new JoseKeysetFactory())
						.providers(KeyEncryptionKeyProvider.of(
								CryptoProperties.PROVIDER_NAME,
								TinkKeyEncryptionKey.builder(CryptoProperties.PROVIDER_NAME)
										.generate(CryptoProperties.KEK_ID)
						))
						.build()
		);
	}

}
