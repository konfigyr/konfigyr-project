package com.konfigyr.identity.authorization.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachingRegisteredClientRepositoryTest {

	@Mock
	RegisteredClientRepository delegate;

	@Mock
	RegisteredClient client;

	ConcurrentMapCache cache;

	CachingRegisteredClientRepository repository;

	@BeforeEach
	void setup() {
		cache = new ConcurrentMapCache("test-client-cache", true);
		repository = new CachingRegisteredClientRepository(cache, delegate);
	}

	@Test
	@DisplayName("should delegate save to the underlying repository")
	void shouldDelegateSave() {
		assertThatNoException().isThrownBy(() -> repository.save(client));

		verify(delegate).save(client);

		assertThat(cache.getNativeCache())
				.isEmpty();
	}

	@Test
	@DisplayName("should return null and cache it when delegate returns no client for findById")
	void shouldReturnNullForUnknownRegistrationId() {
		assertThat(repository.findById("unknown")).isNull();
		assertThat(repository.findById("unknown")).isNull();

		verify(delegate).findById("unknown");

		assertThat(cache.get("id:unknown"))
				.isNotNull();
	}

	@Test
	@DisplayName("should return null and cache it when delegate returns no client for findByClientId")
	void shouldReturnNullForUnknownClientId() {
		assertThat(repository.findByClientId("unknown")).isNull();
		assertThat(repository.findByClientId("unknown")).isNull();

		verify(delegate).findByClientId("unknown");

		assertThat(cache.get("client-id:unknown"))
				.isNotNull();
	}

	@Test
	@DisplayName("should cache registered client on findById and not call delegate on subsequent lookups")
	void shouldCacheFindById() {
		doReturn("test-registration-id").when(client).getId();
		doReturn("kfg-test-client-id").when(client).getClientId();
		doReturn(client).when(delegate).findById("test-registration-id");

		assertThat(repository.findById("test-registration-id"))
				.isSameAs(client);

		assertThat(repository.findById("test-registration-id"))
				.isSameAs(client);

		verify(delegate).findById("test-registration-id");
	}

	@Test
	@DisplayName("should cache registered client on findByClientId and not call delegate on subsequent lookups")
	void shouldCacheFindByClientId() {
		doReturn("test-registration-id").when(client).getId();
		doReturn("kfg-test-client-id").when(client).getClientId();
		doReturn(client).when(delegate).findByClientId("kfg-test-client-id");

		assertThat(repository.findByClientId("kfg-test-client-id"))
				.isSameAs(client);

		assertThat(repository.findByClientId("kfg-test-client-id"))
				.isSameAs(client);

		verify(delegate).findByClientId("kfg-test-client-id");
	}

	@Test
	@DisplayName("should warm client ID cache key after a findById hit")
	void shouldWarmClientIdCacheAfterFindById() {
		doReturn("test-registration-id").when(client).getId();
		doReturn("kfg-test-client-id").when(client).getClientId();
		doReturn(client).when(delegate).findById("test-registration-id");

		assertThat(repository.findById("test-registration-id")).isSameAs(client);
		assertThat(repository.findByClientId("kfg-test-client-id")).isSameAs(client);

		verify(delegate).findById("test-registration-id");
		verify(delegate, never()).findByClientId(anyString());
	}

	@Test
	@DisplayName("should warm registration ID cache key after a findByClientId hit")
	void shouldWarmRegistrationIdCacheAfterFindByClientId() {
		doReturn("test-registration-id").when(client).getId();
		doReturn("kfg-test-client-id").when(client).getClientId();
		doReturn(client).when(delegate).findByClientId("kfg-test-client-id");

		assertThat(repository.findByClientId("kfg-test-client-id")).isSameAs(client);
		assertThat(repository.findById("test-registration-id")).isSameAs(client);

		verify(delegate).findByClientId("kfg-test-client-id");
		verify(delegate, never()).findById(anyString());
	}

}
