package com.konfigyr.security.access;

import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.function.Supplier;

@Slf4j
@NullMarked
@RequiredArgsConstructor
class AccessControlCache {

	private final Cache delegate;

	@Nullable
	AccessControl get(ObjectIdentity identity, Supplier<@Nullable AccessControl> supplier) {
		final Cache.ValueWrapper value = delegate.get(identity);

		if (value != null) {
			return (AccessControl) value.get();
		}

		AccessControl accessControl;

		synchronized (delegate) {
			accessControl = supplier.get();
		}

		set(identity, accessControl);

		return accessControl;
	}

	void set(ObjectIdentity identity, @Nullable AccessControl accessControl) {
		synchronized (delegate) {
			delegate.put(identity, accessControl);
		}
	}

	void evict(ObjectIdentity identity) {
		synchronized (delegate) {
			delegate.evict(identity);
		}
	}

	@Async
	@TransactionalEventListener(
			id = "security.access-control.cache-evict.namespace-deleted",
			classes = NamespaceEvent.Deleted.class
	)
	void on(NamespaceEvent.Deleted event) {
		evict(event);
	}

	@Async
	@TransactionalEventListener(
			id = "security.access-control.cache-evict.member-added",
			classes = NamespaceEvent.MemberAdded.class
	)
	void on(NamespaceEvent.MemberAdded event) {
		evict(event);
	}

	@Async
	@TransactionalEventListener(
			id = "security.access-control.cache-evict.member-updated",
			classes = NamespaceEvent.MemberUpdated.class
	)
	void on(NamespaceEvent.MemberUpdated event) {
		evict(event);
	}

	@Async
	@TransactionalEventListener(
			id = "security.access-control.cache-evict.member-removed",
			classes = NamespaceEvent.MemberRemoved.class
	)
	void on(NamespaceEvent.MemberRemoved event) {
		evict(event);
	}

	@Async
	@TransactionalEventListener(
			id = "security.access-control.cache-evict.application-created",
			classes = NamespaceEvent.ApplicationCreated.class
	)
	void on(NamespaceEvent.ApplicationCreated event) {
		evict(event);
	}

	@Async
	@TransactionalEventListener(
			id = "security.access-control.cache-evict.application-updated",
			classes = NamespaceEvent.ApplicationUpdated.class
	)
	void on(NamespaceEvent.ApplicationUpdated event) {
		evict(event);
	}

	@Async
	@TransactionalEventListener(
			id = "security.access-control.cache-evict.application-removed",
			classes = NamespaceEvent.ApplicationRemoved.class
	)
	void on(NamespaceEvent.ApplicationRemoved event) {
		evict(event);
	}

	private void evict(NamespaceEvent event) {
		final Namespace namespace = event.get();

		log.debug("Evicting access control cache for namespace '{}' triggered by '{}' event",
				namespace.slug(), event.getClass().getSimpleName());

		evict(ObjectIdentity.namespace(namespace.slug()));
	}

}
