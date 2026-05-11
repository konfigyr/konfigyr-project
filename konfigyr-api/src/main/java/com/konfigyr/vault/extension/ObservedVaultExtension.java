package com.konfigyr.vault.extension;

import com.konfigyr.vault.*;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

/**
 * {@link VaultExtension} that adds Micrometer-based observations around {@link Vault} operations.
 * <p>
 * This extension instruments the vault execution lifecycle and provides end-to-end operational
 * visibility for configuration state access, cryptographic operations, repository mutations,
 * and change workflows.
 * <p>
 * The extension decorates the target {@link Vault} with an observed variant that records timing,
 * execution outcomes, and contextual metadata through the configured {@link ObservationRegistry}.
 * <p>
 * The observation boundary intentionally wraps the entire vault operation, including:
 * <ul>
 *     <li>Lock acquisition and contention time</li>
 *     <li>Repository access and mutation operations</li>
 *     <li>Encryption and decryption work</li>
 *     <li>Event publication overhead</li>
 *     <li>Internal validation and orchestration logic</li>
 * </ul>
 *
 * This extension is therefore expected to be the outermost decorator in the {@link VaultExtension}
 * chain. Placing observations outside the locking layer ensures that recorded durations
 * accurately represent the total latency experienced by callers, including time spent waiting
 * on profile-scoped locks.
 *
 * <p>
 * <b>Important security consideration</b>, observations produced by this extension must never
 * expose sensitive configuration data and should avoid recording:
 * <ul>
 *     <li>Unsealed property values</li>
 *     <li>Property names containing sensitive information</li>
 *     <li>Repository contents</li>
 *     <li>Commit payloads or plaintext diffs</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see Vault
 * @see VaultExtension
 * @see ObservationRegistry
 */
@NullMarked
@RequiredArgsConstructor
final class ObservedVaultExtension implements VaultExtension {

	static final String OBSERVATION_NAME_PREFIX = "konfigyr.vault.";

	private final ObservationRegistry registry;

	@Override
	public Vault extend(Vault vault) {
		return new ObservedVault(vault, registry);
	}

	static final class ObservedVault extends AbstractDelegatingVault {

		private final ObservationRegistry registry;

		ObservedVault(Vault delegate, ObservationRegistry registry) {
			super(delegate);
			this.registry = registry;
		}

		@Override
		public Properties state() {
			return createObservation(ObservedOperation.STATE)
					.observe(super::state);
		}

		@Override
		public Map<String, String> unseal() {
			return createObservation(ObservedOperation.UNSEAL)
					.observe(() -> super.unseal());
		}

		@Override
		public PropertyValue seal(PropertyValue property) {
			return createObservation(ObservedOperation.SEAL_PROPERTY)
					.observe(() -> super.seal(property));
		}

		@Override
		public PropertyValue unseal(PropertyValue property) {
			return createObservation(ObservedOperation.UNSEAL_PROPERTY)
					.observe(() -> super.unseal(property));
		}

		@Override
		public ApplyResult apply(PropertyChanges changes) {
			return createObservation(ObservedOperation.APPLY)
					.observe(() -> super.apply(changes));
		}

		@Override
		public ChangeRequest submit(PropertyChanges changes) {
			return createObservation(ObservedOperation.SUBMIT)
					.observe(() -> super.submit(changes));
		}

		@Override
		public ApplyResult merge(ChangeRequest changeRequest) {
			return createObservation(ObservedOperation.MERGE)
					.highCardinalityKeyValue("konfigyr.vault.change-request", changeRequest.id().serialize())
					.observe(() -> super.merge(changeRequest));
		}

		@Override
		public ChangeRequest discard(ChangeRequest changeRequest) {
			return createObservation(ObservedOperation.DISCARD)
					.highCardinalityKeyValue("konfigyr.vault.change-request", changeRequest.id().serialize())
					.observe(() -> super.discard(changeRequest));
		}

		@Override
		public void close() throws Exception {
			createObservation(ObservedOperation.CLOSE)
					.observeChecked(super::close);
		}

		private Observation createObservation(ObservedOperation operation) {
			final String service = service().id().serialize();
			final String profile = profile().id().serialize();

			return Observation.createNotStarted(operation.observationName(), registry)
					.contextualName(operation.contextualName(service, profile))
					.highCardinalityKeyValue("konfigyr.namespace.service", service)
					.highCardinalityKeyValue("konfigyr.vault.profile", profile);
		}

	}

	enum ObservedOperation {
		STATE("state", "reading state of vault profile '%s' for service '%s'"),
		UNSEAL("unseal", "unsealing state of vault profile '%s' for service '%s'"),
		SEAL_PROPERTY("seal-property", "sealing value of vault profile '%s' for service '%s'"),
		UNSEAL_PROPERTY("unseal-property", "unsealing value of vault profile '%s' for service '%s'"),
		APPLY("apply", "applying changes to vault profile '%s' for service '%s'"),
		SUBMIT("submit", "submitting changes for review to vault profile '%s' for service '%s'"),
		MERGE("merge", "merging proposed changes to vault profile '%s' for service '%s'"),
		DISCARD("discard", "discarding proposed changes to vault profile '%s' for service '%s'"),
		CLOSE("close", "closing vault profile '%s' for service '%s'");

		private final String name;
		private final String contextualNameTemplate;

		ObservedOperation(String operation, String contextualNameTemplate) {
			this.name = OBSERVATION_NAME_PREFIX + operation;
			this.contextualNameTemplate = contextualNameTemplate;
		}

		String observationName() {
			return name;
		}

		String contextualName(String service, String profile) {
			return contextualNameTemplate.formatted(service, profile);
		}
	}

}
