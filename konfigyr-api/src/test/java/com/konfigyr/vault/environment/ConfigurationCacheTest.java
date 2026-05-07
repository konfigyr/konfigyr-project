package com.konfigyr.vault.environment;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceEvent;
import com.konfigyr.vault.Profile;
import com.konfigyr.vault.ProfileEvent;
import com.konfigyr.vault.Properties;
import com.konfigyr.vault.VaultEvent;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.function.ThrowingSupplier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationCacheTest {

	final ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Mock(strictness = Mock.Strictness.LENIENT)
	Service service;

	@Mock(strictness = Mock.Strictness.LENIENT)
	Profile profile;

	@Mock
	ThrowingSupplier<Properties> supplier;

	Properties properties;

	ConfigurationCache cache;

	@BeforeEach
	void setup() throws IOException {
		doReturn(EntityId.from(542)).when(profile).id();
		doReturn(EntityId.from(245)).when(profile).service();
		doReturn(EntityId.from(245)).when(service).id();

		cache = new ConfigurationCache("maximumWeight=128000,expireAfterAccess=5h");
		properties = Properties.from(resourceLoader.getResource("classpath:fixtures/test-properties-sealed.properties"));
	}

	@Test
	@DisplayName("should not load configuration state if configuration state is cached")
	void shouldRetrieveCachedStateIfPresent() {
		cache.put(service, profile, properties);

		assertThat(cache.get(service, profile, supplier))
				.as("should return cached configuration state")
				.isSameAs(properties);

		verifyNoInteractions(supplier);
	}

	@Test
	@DisplayName("should load configuration state from loader and cache it for subsequent usage")
	void shouldLoadAndCacheConfigurationState() {
		doReturn(properties).when(supplier).get();

		assertThat(cache.get(service, profile, supplier))
				.as("should return loaded configuration state")
				.isSameAs(properties);

		assertThat(cache.get(service, profile, supplier))
				.as("should return cached configuration state")
				.isSameAs(properties);

		verify(supplier).get();
	}

	@Test
	@DisplayName("should check if configuration state is present in the cache")
	void shouldCheckIfStateIsCached() {
		assertThat(cache.has(service, profile))
				.as("should not have any cached configuration states")
				.isFalse();

		cache.put(service, profile, properties);

		assertThat(cache.has(service, profile))
				.as("should have a cached configuration state")
				.isTrue();
	}

	@Test
	@DisplayName("should rethrow configuration state loader exceptions")
	void shouldRethrowLoaderExceptions() {
		final var cause = new IllegalStateException("loader failure");

		doThrow(cause).when(supplier).get();

		assertThatException()
				.as("should rethrow loader exception when state is not cached")
				.isThrownBy(() -> cache.get(service, profile, supplier))
				.isEqualTo(cause);
	}

	@Test
	@DisplayName("should evict all configuration states that are cached for the removed service")
	void shouldEvictAllProfilesForService() {
		final var removed = mock(Service.class);
		final var development = mock(Profile.class);
		final var staging = mock(Profile.class);

		doReturn(EntityId.from(900)).when(removed).id();
		doReturn(EntityId.from(911)).when(development).id();
		doReturn(EntityId.from(912)).when(staging).id();

		assertThat(cache.put(service, profile, properties))
				.isSameAs(properties);
		assertThat(cache.put(removed, development, properties))
				.isSameAs(properties);
		assertThat(cache.put(removed, staging, properties))
				.isSameAs(properties);

		assertThatNoException().isThrownBy(() -> cache.on(new ServiceEvent.Deleted(removed)));

		assertThat(cache.has(service, profile))
				.as("should not remove any configuration states for unaffected service")
				.isTrue();
		assertThat(cache.has(removed, development))
				.as("should not have any cached configuration state for the removed service")
				.isFalse();
		assertThat(cache.has(removed, staging))
				.as("should not have any cached configuration state for the removed service")
				.isFalse();
	}

	@Test
	@DisplayName("should evict configuration state for profile that was updated")
	void shouldEvictCachedProfileWhenUpdated() {
		final var updated = mock(Profile.class);
		doReturn(EntityId.from(4321)).when(updated).id();
		doReturn(EntityId.from(245)).when(updated).service();

		assertThat(cache.put(service, updated, properties))
				.isSameAs(properties);
		assertThat(cache.put(service, profile, properties))
				.isSameAs(properties);

		assertThatNoException().isThrownBy(() -> cache.on(new VaultEvent.ChangesApplied(updated, mock())));

		assertThat(cache.has(service, updated))
				.as("should remove configuration state from cache for the updated profile")
				.isFalse();
		assertThat(cache.has(service, profile))
				.as("should not remove any configuration states for unaffected profile")
				.isTrue();
	}

	@Test
	@DisplayName("should evict configuration state for profile that was removed")
	void shouldEvictCachedProfileWhenRemoved() {
		final var removed = mock(Profile.class);
		doReturn(EntityId.from(4321)).when(removed).id();
		doReturn(EntityId.from(245)).when(removed).service();

		assertThat(cache.put(service, removed, properties))
				.isSameAs(properties);
		assertThat(cache.put(service, profile, properties))
				.isSameAs(properties);

		assertThatNoException().isThrownBy(() -> cache.on(new ProfileEvent.Deleted(removed)));

		assertThat(cache.has(service, removed))
				.as("should remove configuration state from cache for the deleted profile")
				.isFalse();
		assertThat(cache.has(service, profile))
				.as("should not remove any configuration states for unaffected profile")
				.isTrue();
	}

	@Test
	@DisplayName("should provide a custom meter binder implementation to monitor cache")
	void shouldMonitorCache() {
		final var tags = Tags.of("cache", ConfigurationCache.CACHE_NAME);
		final var registry = new SimpleMeterRegistry();

		cache.has(service, profile);
		cache.put(service, profile, properties);
		cache.has(service, profile);

		assertThatNoException().isThrownBy(() -> cache.bindTo(registry));

		assertThat(registry.get("cache.size").tags(tags).gauge())
				.as("should have a metric for the cache size")
				.returns(1.0, Gauge::value);
	}

}
