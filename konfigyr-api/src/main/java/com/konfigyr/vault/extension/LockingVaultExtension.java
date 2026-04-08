package com.konfigyr.vault.extension;

import com.konfigyr.entity.EntityId;
import com.konfigyr.vault.*;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of the {@link VaultExtension} that provides per-(service, profile) read/write
 * coordination for {@link Vault} objects.
 * <p>
 * The returned vault extension allows multiple readers concurrently but prevents multiple writers
 * from performing state updates. It is also important to note that readers are blocked while a
 * write operation is in progress.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public final class LockingVaultExtension implements VaultExtension {

	final ConcurrentHashMap<VaultLockKey, VaultLockEntry> locks = new ConcurrentHashMap<>();

	@Override
	public Vault extend(Vault vault) {
		final VaultLockKey key = new VaultLockKey(vault);

		final VaultLockEntry entry = locks.compute(key, (ignore, existing) -> {
			if (existing == null) {
				existing = new VaultLockEntry(new AtomicInteger(0), new ReentrantReadWriteLock(true));
			}
			return existing;
		});

		return new LockingVault(vault, entry.acquire(), () -> {
			// When the reference count reaches zero and no threads are holding the lock, remove it from the registry
			if (entry.release() && entry.inactive()) {
				locks.remove(key);
			}
		});
	}

	/**
	 * Identifies a unique vault instance by service and profile.
	 * <p>
	 * This key is used to scope read/write locks so that concurrent access to the same
	 * (service, profile) pair is coordinated.
	 *
	 * @param service the owning service identifier
	 * @param profile the profile identifier
	 */
	record VaultLockKey(EntityId service, EntityId profile) {

		VaultLockKey(Vault vault) {
			this(vault.service().id(), vault.profile().id());
		}

	}

	/**
	 * Internal record representing the vault lock and its usage counter.
	 *
	 * @param counter the lock usage counter
	 * @param lock the reentrant read/write lock
	 */
	private record VaultLockEntry(AtomicInteger counter, ReentrantReadWriteLock lock) {

		/**
		 * Acquires a lock entry for the given key and increments its reference count.
		 *
		 * @return the associated lock entry
		 */
		ReadWriteLock acquire() {
			counter.incrementAndGet();
			return lock;
		}

		/**
		 * Checks if there are no threads are holding the lock. This is used to remove
		 * this entry from the vault lock registry.
		 *
		 * @return {@code true} if no write or read locks are active, {@code false} otherwise
		 */
		boolean inactive() {
			return !lock.isWriteLocked() && lock.getReadLockCount() == 0;
		}

		/**
		 * Releases a previously acquired lock entry.
		 * <p>
		 * When the reference count reaches zero this method would return {@code true}.
		 *
		 * @return {@code true} if the last lock was released, {@code false} otherwise
		 */
		boolean release() {
			return counter.decrementAndGet() == 0;
		}

	}

	/**
	 * Vault decorator that enforces read/write locking semantics. All read operations acquire a
	 * read lock while write operations acquire a write lock. When this vault is closed, the
	 * lock state is updated until the last locked vault is released.
	 */
	@NullMarked
	private static final class LockingVault extends AbstractDelegatingVault {

		private final ReadWriteLock lock;
		private final Runnable hook;

		private LockingVault(Vault delegate, ReadWriteLock lock, Runnable hook) {
			super(delegate);
			this.lock = lock;
			this.hook = hook;
		}

		@Override
		public Properties state() {
			lock.readLock().lock();
			try {
				return delegate.state();
			} finally {
				lock.readLock().unlock();
			}
		}

		@Override
		public Map<String, String> unseal() {
			lock.readLock().lock();
			try {
				return delegate.unseal();
			} finally {
				lock.readLock().unlock();
			}
		}

		@Override
		public PropertyValue seal(PropertyValue property) {
			lock.readLock().lock();
			try {
				return delegate.seal(property);
			} finally {
				lock.readLock().unlock();
			}
		}

		@Override
		public PropertyValue unseal(PropertyValue property) {
			lock.readLock().lock();
			try {
				return delegate.unseal(property);
			} finally {
				lock.readLock().unlock();
			}
		}

		@Override
		public ApplyResult apply(PropertyChanges changes) {
			lock.writeLock().lock();
			try {
				return delegate.apply(changes);
			} finally {
				lock.writeLock().unlock();
			}
		}

		@Override
		public Vault submit(PropertyChanges changes) {
			lock.writeLock().lock();
			try {
				return delegate.submit(changes);
			} finally {
				lock.writeLock().unlock();
			}
		}

		@Override
		public void close() throws Exception {
			try {
				super.close();
			} finally {
				hook.run();
			}
		}
	}
}
