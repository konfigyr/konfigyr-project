package com.konfigyr.security.access;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceApplication;
import com.konfigyr.namespace.NamespaceEvent;
import com.konfigyr.namespace.NamespaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessControlCacheTest {

	@Mock
	Supplier<AccessControl> repository;

	@Mock
	AccessControl controls;

	Cache delegate;

	AccessControlCache cache;

	@BeforeEach
	void setup() {
		delegate = new ConcurrentMapCache("test-cache");
		cache = new AccessControlCache(delegate);
	}

	@Test
	@DisplayName("should retrieve value from supplier when cache is not present")
	void retrieveValueFromRepository() {
		final var identity = ObjectIdentity.namespace("john-doe");

		doReturn(controls).when(repository).get();

		assertThat(cache.get(identity, repository))
				.isSameAs(controls);

		assertThat(cache.get(identity, repository))
				.isSameAs(controls);

		verify(repository).get();
	}

	@Test
	@DisplayName("should retrieve value from cache and not invoke the cache supplier function")
	void retrieveCachedValue() {
		final var identity = ObjectIdentity.namespace("john-doe");
		cache.set(identity, controls);

		assertThat(cache.get(identity, repository))
				.isSameAs(controls);

		verifyNoInteractions(repository);
	}

	@Test
	@DisplayName("should store `null` values in the cache")
	void storeNullValues() {
		final var identity = ObjectIdentity.namespace("john-doe");

		assertThat(cache.get(identity, repository))
				.isNull();

		assertThat(cache.get(identity, repository))
				.isNull();

		verify(repository).get();
	}

	@Test
	@DisplayName("should clear cache for namespace object identity when a namespace is removed")
	void evictOnNamespaceRemoved() {
		final var namespace = prepareCacheForEviction();

		assertThatNoException().isThrownBy(() -> cache.on(new NamespaceEvent.Deleted(namespace)));

		assertThat(delegate.get(ObjectIdentity.namespace(namespace.slug())))
				.as("Cache should be cleared")
				.isNull();
	}

	@Test
	@DisplayName("should clear cache for namespace object identity when a namespace member is added")
	void evictOnNamespaceMemberAdded() {
		final var namespace = prepareCacheForEviction();

		assertThatNoException().isThrownBy(() -> cache.on(new NamespaceEvent.MemberAdded(
				namespace, EntityId.from(1), NamespaceRole.USER
		)));

		assertThat(delegate.get(ObjectIdentity.namespace(namespace.slug())))
				.as("Cache should be cleared")
				.isNull();
	}

	@Test
	@DisplayName("should clear cache for namespace object identity when a namespace member is updated")
	void evictOnNamespaceMemberUpdated() {
		final var namespace = prepareCacheForEviction();

		assertThatNoException().isThrownBy(() -> cache.on(new NamespaceEvent.MemberUpdated(
				namespace, EntityId.from(1), NamespaceRole.ADMIN
		)));

		assertThat(delegate.get(ObjectIdentity.namespace(namespace.slug())))
				.as("Cache should be cleared")
				.isNull();
	}

	@Test
	@DisplayName("should clear cache for namespace object identity when a namespace member is removed")
	void evictOnNamespaceMemberRemoved() {
		final var namespace = prepareCacheForEviction();

		assertThatNoException().isThrownBy(() -> cache.on(new NamespaceEvent.MemberRemoved(namespace, EntityId.from(1))));

		assertThat(delegate.get(ObjectIdentity.namespace(namespace.slug())))
				.as("Cache should be cleared")
				.isNull();
	}

	@Test
	@DisplayName("should clear cache for namespace object identity when a namespace application is created")
	void evictOnNamespaceApplicationCreated() {
		final var namespace = prepareCacheForEviction();
		final var application = mock(NamespaceApplication.class);

		assertThatNoException().isThrownBy(() -> cache.on(new NamespaceEvent.ApplicationCreated(namespace, application)));

		assertThat(delegate.get(ObjectIdentity.namespace(namespace.slug())))
				.as("Cache should be cleared")
				.isNull();
	}

	@Test
	@DisplayName("should clear cache for namespace object identity when a namespace application is updated")
	void evictOnNamespaceApplicationUpdated() {
		final var namespace = prepareCacheForEviction();
		final var application = mock(NamespaceApplication.class);

		assertThatNoException().isThrownBy(() -> cache.on(new NamespaceEvent.ApplicationUpdated(namespace, application)));

		assertThat(delegate.get(ObjectIdentity.namespace(namespace.slug())))
				.as("Cache should be cleared")
				.isNull();
	}

	@Test
	@DisplayName("should clear cache for namespace object identity when a namespace application is removed")
	void evictOnNamespaceApplicationRemoved() {
		final var namespace = prepareCacheForEviction();
		final var application = mock(NamespaceApplication.class);

		assertThatNoException().isThrownBy(() -> cache.on(new NamespaceEvent.ApplicationRemoved(namespace, application)));

		assertThat(delegate.get(ObjectIdentity.namespace(namespace.slug())))
				.as("Cache should be cleared")
				.isNull();
	}

	Namespace prepareCacheForEviction() {
		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(1)).when(namespace).id();
		doReturn("konfigyr").when(namespace).slug();

		final var identity = ObjectIdentity.namespace(namespace.slug());

		assertThatNoException()
				.isThrownBy(() -> delegate.put(identity, controls));

		assertThat(delegate.get(identity))
				.as("Cache should be set for object identity")
				.isNotNull();

		return namespace;
	}

}
