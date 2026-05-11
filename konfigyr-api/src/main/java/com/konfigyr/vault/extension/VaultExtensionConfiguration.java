package com.konfigyr.vault.extension;

import com.konfigyr.vault.VaultExtension;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Configuration class for core {@link VaultExtension VaultExtensions}.
 * <p>
 * This configuration registers the default extension chain applied to every resolved
 * {@link com.konfigyr.vault.Vault} instance. Extensions are ordered explicitly to ensure correct
 * operational semantics, observability, concurrency guarantees, and event consistency.
 * <p>
 * The resulting invocation chain is:
 * <pre>
 * ObservedVaultExtension
 *     → LockingVaultExtension
 *         → PublishingVaultExtension
 *             → Actual Vault
 * </pre>
 *
 * <strong>1. Observation Layer</strong>
 * <p>
 * The {@link ObservedVaultExtension} is applied first (outermost layer) so that Micrometer
 * observations encompass the entire lifecycle of a vault operation, including:
 *
 * <ul>
 *     <li>Lock acquisition and contention time</li>
 *     <li>Repository interactions</li>
 *     <li>Event publication overhead</li>
 *     <li>Internal processing latency</li>
 * </ul>
 *
 * Placing observations outside the locking layer ensures that metrics reflect the real end-to-end
 * duration experienced by callers rather than only the internal execution time after lock acquisition.
 * <p>
 * <strong>2. Locking Layer</strong>
 * <p>
 * The {@link LockingVaultExtension} is applied after observations and before event publication. This
 * establishes the lock boundary as the authoritative consistency boundary for all state mutations.
 * <p>
 * The lock protects:
 *
 * <ul>
 *     <li>Repository state transitions</li>
 *     <li>Branch and revision updates</li>
 *     <li>Change request lifecycle mutations</li>
 *     <li>Event publication ordering</li>
 * </ul>
 *
 * Keeping event publication inside the lock guarantees that emitted events remain causally ordered
 * with the underlying repository mutations and prevents race conditions between concurrent apply,
 * merge, or discard operations.
 * <p>
 * <strong>3. Event Publishing Layer</strong>
 * <p>
 * The {@link PublishingVaultExtension} is placed closest to the actual vault implementation so that
 * events are only emitted after a successful mutation has completed.
 * <p>
 * This ensures:
 *
 * <ul>
 *     <li>Only committed state transitions produce events</li>
 *     <li>Failed operations do not emit misleading events</li>
 *     <li>Published events reflect the final resulting state</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see VaultExtension
 */
@Configuration(proxyBeanMethods = false)
public class VaultExtensionConfiguration {

	@Bean
	@Order(1)
	VaultExtension observedVaultExtension(ObservationRegistry observationRegistry) {
		return new ObservedVaultExtension(observationRegistry);
	}

	@Bean
	@Order(2)
	VaultExtension lockingVaultExtension() {
		return new LockingVaultExtension();
	}

	@Bean
	@Order(3)
	VaultExtension publishingVaultExtension(ApplicationEventPublisher eventPublisher) {
		return new PublishingVaultExtension(eventPublisher);
	}

}
