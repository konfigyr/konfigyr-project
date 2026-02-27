package com.konfigyr.vault.extension;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.vault.Profile;
import com.konfigyr.vault.PropertyChanges;
import com.konfigyr.vault.Vault;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.quality.Strictness;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LockingVaultExtensionTest {

	LockingVaultExtension extension = new LockingVaultExtension();

	@Test
	@DisplayName("should acquire and release lock")
	void acquireAndRelease() throws Exception {
		final var vault = createVault(1, 1);

		vault.close();

		assertThat(extension.locks)
				.isEmpty();
	}

	@Test
	@DisplayName("should make sure that locks are unique per service and profile")
	void locksShouldBeUnique() throws Exception {
		final var first = createVault(1, 1);
		final var second = createVault(2, 1);
		final var third = createVault(1, 2);

		assertThat(extension.locks)
				.hasSize(3)
				.containsOnlyKeys(
						new LockingVaultExtension.VaultLockKey(first),
						new LockingVaultExtension.VaultLockKey(second),
						new LockingVaultExtension.VaultLockKey(third)
				);

		first.close();
		assertThat(extension.locks)
				.hasSize(2)
				.containsOnlyKeys(
						new LockingVaultExtension.VaultLockKey(second),
						new LockingVaultExtension.VaultLockKey(third)
				);

		second.close();
		assertThat(extension.locks)
				.hasSize(1)
				.containsOnlyKeys(
						new LockingVaultExtension.VaultLockKey(third)
				);

		third.close();
		assertThat(extension.locks)
				.isEmpty();
	}

	@Test
	@DisplayName("should release locks when the last locked vault is closed")
	void refCountingWorks() throws Exception {
		final var first = createVault(1, 1);
		final var second = createVault(1, 1);

		first.close();
		assertThat(extension.locks)
				.hasSize(1);

		second.close();
		assertThat(extension.locks)
				.isEmpty();
	}

	@Test
	@DisplayName("should allow multiple readers when reading the vault state")
	void concurrentReadersAllowed() throws Exception {
		final var vault = createVault(1, 1);

		final var executor = Executors.newFixedThreadPool(3);
		final var latch = new CountDownLatch(3);

		Runnable reader = () -> {
			vault.state();
			latch.countDown();
		};

		executor.submit(reader);
		executor.submit(reader);
		executor.submit(reader);

		assertThat(latch.await(2, TimeUnit.SECONDS))
				.as("Readers should execute concurrently")
				.isTrue();

		vault.close();
		executor.shutdown();
	}

	@Test
	@DisplayName("should block readers when writing the vault state")
	void writerBlocksReaders() throws Exception {
		final var vault = createVault(1, 1);

		final var executor = Executors.newFixedThreadPool(3);

		final var writerStarted = new CountDownLatch(1);
		final var writerFinish = new CountDownLatch(1);
		final var executed = new AtomicBoolean(false);

		executor.submit(() -> {
			writerStarted.countDown();
			vault.apply(mock(PropertyChanges.class));
			writerFinish.countDown();
		});

		writerStarted.await();

		executor.submit(() -> {
			vault.state();
			executed.set(true);
		});

		Thread.sleep(100);

		assertThat(executed.get())
				.as("Reader must block while writer holds lock")
				.isFalse();

		assertThat(writerFinish.await(2, TimeUnit.SECONDS))
				.as("Writer should be finished")
				.isTrue();

		vault.close();
		executor.shutdown();
	}

	@Test
	@DisplayName("should block updates when writing the vault state")
	void writerBlocksUpdates() throws Exception {
		final var vault = createVault(1, 1);

		final var executor = Executors.newFixedThreadPool(3);

		final var writerStarted = new CountDownLatch(1);
		final var writerFinish = new CountDownLatch(1);
		final var executed = new AtomicBoolean(false);

		executor.submit(() -> {
			writerStarted.countDown();
			vault.apply(mock(PropertyChanges.class));
			writerFinish.countDown();
		});

		writerStarted.await();

		executor.submit(() -> {
			vault.submit(mock(PropertyChanges.class));
			executed.set(true);
		});

		Thread.sleep(100);

		assertThat(executed.get())
				.as("Update must be blocked while writer holds lock")
				.isFalse();

		assertThat(writerFinish.await(2, TimeUnit.SECONDS))
				.as("Writer should be finished")
				.isTrue();

		vault.close();
		executor.shutdown();
	}

	@Test
	@DisplayName("do not remove the lock entry while it is locked")
	void lockNotRemovedWhileHeld() throws Exception {
		final var vault = createVault(1, 1);

		final var executor = Executors.newSingleThreadExecutor();
		final var latch = new CountDownLatch(1);

		executor.submit(() -> {
			vault.apply(mock(PropertyChanges.class));
			latch.countDown();
		});

		Thread.sleep(100);

		vault.close();

		assertThat(extension.locks)
				.as("Lock should not be removed while held")
				.hasSize(1);

		assertThat(latch.await(2, TimeUnit.SECONDS))
				.as("Writer should be finished")
				.isTrue();

		executor.shutdown();
	}

	private Vault createVault(long serviceId, long profileId) {
		final var service = mock(Service.class);
		doReturn(EntityId.from(serviceId)).when(service).id();

		final var profile = mock(Profile.class);
		doReturn(EntityId.from(profileId)).when(profile).id();

		final var vault = mock(Vault.class, withSettings().strictness(Strictness.LENIENT));
		doReturn(service).when(vault).service();
		doReturn(profile).when(vault).profile();

		final var delayed = new AnswersWithDelay(200, Answers.RETURNS_SMART_NULLS);
		doAnswer(delayed).when(vault).state();
		doAnswer(delayed).when(vault).apply(any());
		doAnswer(delayed).when(vault).submit(any());

		return extension.extend(vault);
	}

}
