package com.konfigyr.identity.authorization.jwk;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RepositoryKeySourceTest {

	private static final JWKSelector ALL_SELECTOR = new JWKSelector(new JWKMatcher.Builder().build());

	@Mock
	KeyRepository repository;

	JWK key;

	RepositoryKeySource source;

	@BeforeEach
	void setup() {
		source = new RepositoryKeySource(repository);
		key = createKey("active-key", RepositoryKeySource.CRYPTO_PERIOD);
	}

	@Test
	@DisplayName("should fail to retrieve JWK set on non-initialized source")
	void shouldWaitForInitialization() {
		assertThatThrownBy(() -> source.get(ALL_SELECTOR, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWK source has not been initialized");

		verifyNoInteractions(repository);
	}

	@Test
	@DisplayName("should initialize JWK Source when repository does not contain any keys")
	void shouldInitializeKeys() throws Exception {
		doReturn(key).when(repository).create(KeyAlgorithm.RS256, RepositoryKeySource.CRYPTO_PERIOD);

		assertThatNoException().isThrownBy(source::afterPropertiesSet);

		assertThat(source.get(ALL_SELECTOR, null))
				.hasSize(1)
				.containsExactly(key);

		verify(repository).get();
		verify(repository).create(KeyAlgorithm.RS256, RepositoryKeySource.CRYPTO_PERIOD);
		verify(repository, never()).delete(any());
	}

	@Test
	@DisplayName("should initialize JWK source when repository already contains non-expired keys")
	void shouldInitializeSource() throws Exception {
		doReturn(List.of(key)).when(repository).get();

		assertThatNoException().isThrownBy(source::afterPropertiesSet);

		assertThat(source.get(ALL_SELECTOR, null))
				.hasSize(1)
				.containsExactly(key);

		verify(repository).get();
		verify(repository, never()).create(any(), any());
		verify(repository, never()).delete(any());
	}

	@Test
	@DisplayName("should fail to initialize JWK source when reading keys")
	void shouldFailToInitializeSource() {
		doThrow(InternalAuthenticationServiceException.class).when(repository).get();

		assertThatThrownBy(source::afterPropertiesSet)
				.isInstanceOf(InternalAuthenticationServiceException.class);

		verify(repository).get();
		verify(repository, never()).create(any(), any());
		verify(repository, never()).delete(any());
	}

	@Test
	@DisplayName("should fail to initialize JWK source when signing key can not be created")
	void shouldFailToInitializeSigningKey() {
		doThrow(InternalAuthenticationServiceException.class).when(repository).create(any(), any());

		assertThatThrownBy(source::afterPropertiesSet)
				.isInstanceOf(InternalAuthenticationServiceException.class);

		verify(repository).get();
		verify(repository).create(any(), any());
		verify(repository, never()).delete(any());
	}

	@Test
	@DisplayName("should not remove non-expired keys")
	void shouldNotRemoveNonExpiredKeys() throws Exception {
		doReturn(List.of(key)).when(repository).get();

		assertThatNoException().isThrownBy(source::reload);

		assertThat(source.get(ALL_SELECTOR, null))
				.hasSize(1)
				.containsExactly(key);

		verify(repository).get();
		verify(repository, never()).create(any(), any());
		verify(repository, never()).delete(any());
	}

	@Test
	@DisplayName("should remove soon to be expired keys")
	void shouldRemoveSoonToBeExpiredKeys() throws Exception {
		final var expired = createKey("expired", Duration.ofMinutes(60));

		doReturn(List.of(expired)).when(repository).get();
		doReturn(key).when(repository).create(KeyAlgorithm.RS256, RepositoryKeySource.CRYPTO_PERIOD);

		assertThatNoException().isThrownBy(source::reload);

		assertThat(source.get(ALL_SELECTOR, null))
				.hasSize(1)
				.containsExactly(key);

		verify(repository).get();
		verify(repository).create(KeyAlgorithm.RS256, RepositoryKeySource.CRYPTO_PERIOD);
		verify(repository).delete("expired");
	}

	@Test
	@DisplayName("should remove expired keys")
	void shouldRemoveExpiredKeys() throws Exception {
		final var expired = createKey("expired", Duration.ZERO);

		doReturn(List.of(expired)).when(repository).get();
		doReturn(key).when(repository).create(KeyAlgorithm.RS256, RepositoryKeySource.CRYPTO_PERIOD);

		assertThatNoException().isThrownBy(source::reload);

		assertThat(source.get(ALL_SELECTOR, null))
				.hasSize(1)
				.containsExactly(key);

		verify(repository).get();
		verify(repository).create(KeyAlgorithm.RS256, RepositoryKeySource.CRYPTO_PERIOD);
		verify(repository).delete(eq("expired"));
	}

	@Test
	@DisplayName("should assert key rotation scenario")
	void shouldAssertKeyRotationScenario() throws Exception {
		/* create initial key */
		final var initial = createKey("initial", RepositoryKeySource.CRYPTO_PERIOD);
		doReturn(initial).when(repository).create(KeyAlgorithm.RS256, RepositoryKeySource.CRYPTO_PERIOD);

		assertThatNoException().isThrownBy(source::reload);

		assertThat(source.get(ALL_SELECTOR, null))
				.hasSize(1)
				.containsExactly(initial);

		/* 4 months passed, initial key is not yet scheduled for rotation */
		setupExpirationTimeForKey(initial, RepositoryKeySource.CRYPTO_PERIOD.minusMonths(4));
		doReturn(List.of(initial)).when(repository).get();

		assertThatNoException().isThrownBy(source::reload);

		assertThat(source.get(ALL_SELECTOR, null))
				.hasSize(1)
				.containsExactly(initial);

		/* 1 year passed, initial key should be rotated */
		final var rotated = createKey("rotated", RepositoryKeySource.CRYPTO_PERIOD);
		doReturn(rotated).when(repository).create(KeyAlgorithm.RS256, RepositoryKeySource.CRYPTO_PERIOD);

		setupExpirationTimeForKey(initial, Duration.ofMinutes(45));
		doReturn(List.of(initial)).when(repository).get();

		assertThatNoException().isThrownBy(source::reload);

		assertThat(source.get(ALL_SELECTOR, null))
				.hasSize(1)
				.containsExactly(rotated);
	}

	static JWK createKey(@NonNull String id, @Nullable TemporalAmount expiration) {
		final var timestamp = Instant.now();

		final JWK key = mock(JWK.class, withSettings()
				.strictness(Strictness.LENIENT)
				.name("JWK(" + id + ", issued=" + timestamp + " expiration=" + expiration + ")")
		);

		doReturn(id).when(key).getKeyID();
		doReturn(Date.from(timestamp)).when(key).getIssueTime();

		if (expiration != null) {
			setupExpirationTimeForKey(key, expiration);
		}

		return key;
	}

	static void setupExpirationTimeForKey(@NonNull JWK key, @NonNull TemporalAmount expiration) {
		final ZonedDateTime expirationTime = ZonedDateTime.now().plus(expiration);
		doReturn(Date.from(expirationTime.toInstant())).when(key).getExpirationTime();
	}

}
