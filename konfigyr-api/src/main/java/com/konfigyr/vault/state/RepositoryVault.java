package com.konfigyr.vault.state;


import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.namespace.Service;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.vault.*;
import lombok.Builder;
import org.apache.commons.collections4.OrderedMapIterator;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Builder
final class RepositoryVault implements Vault {

	private final Service service;
	private final Profile profile;
	private final AuthenticatedPrincipal author;
	private final StateRepository repository;
	private final KeysetOperations keysetOperations;

	private volatile Properties state;

	@NonNull
	@Override
	public Service service() {
		return service;
	}

	@NonNull
	@Override
	public Profile profile() {
		return profile;
	}

	/**
	 * Unseals the encrypted property values using the configured {@link KeysetOperations} and returns
	 * them as a {@link Map}.
	 *
	 * @return the unsealed property values, never {@literal null}.
	 */
	@NonNull
	@Override
	public Map<String, String> unseal() {
		final Properties state = state();
		final OrderedMapIterator<String, PropertyValue> iterator = state.iterator();

		final Map<String, String> properties = new LinkedHashMap<>(state.size());
		while (iterator.hasNext()) {
			final String name = iterator.next();
			final PropertyValue value = iterator.getValue().unseal(keysetOperations);

			properties.put(name, new String(value.get().array(), StandardCharsets.UTF_8));
		}

		return Collections.unmodifiableMap(properties);
	}

	/**
	 * Returns the current configuration state of the vault.
	 *
	 * @return the properties that reflect the current state of the vault.
	 */
	@NonNull
	@Override
	public Properties state() {
		if (state == null) {
			try {
				state = Properties.from(repository.get(profile));
			} catch (IOException ex) {
				throw new RepositoryStateException(RepositoryStateException.ErrorCode.CORRUPTED_STATE,
						"Failed read repository configuration state for '%s' profile of Service(%s, %s)".formatted(
								profile.slug(), service.id(), service.slug()), ex);
			}
		}
		return state;
	}

	@NonNull
	@Override
	public ApplyResult apply(@NonNull PropertyChanges changes) {
		if (profile.policy() == ProfilePolicy.IMMUTABLE) {
			throw ProfilePolicyViolationException.immutableProfile(profile);
		}
		if (profile.policy() == ProfilePolicy.PROTECTED) {
			throw ProfilePolicyViolationException.protectedProfile(profile);
		}

		final Properties current = state();
		final Properties updated = current.apply(changes, keysetOperations);

		final Changeset changeset = new Changeset(author, updated, changes);

		final MergeOutcome updateOutcome = repository.update(profile, changeset);

		if (!updateOutcome.isApplied()) {
			repository.discard(profile, updateOutcome.branch());

			throw new IllegalStateException("Failed to prepare changeset for profile '%s' of Service(%s, %s) due to: %s"
					.formatted(profile.slug(), service.id(), service.slug(), updateOutcome));
		}

		final MergeOutcome mergeOutcome = repository.merge(profile, updateOutcome.branch());

		if (mergeOutcome.isConflicting()) {
			repository.discard(profile, updateOutcome.branch());

			Assert.state(mergeOutcome.conflicts() != null, "Merge conflicts must not be null");
			throw new ConflictingProfileStateException(profile, mergeOutcome.conflicts());
		} else if (mergeOutcome.isUnknown()) {
			repository.discard(profile, updateOutcome.branch());

			throw new IllegalStateException("Failed to apply changes to profile '%s' of Service(%s, %s) due to: %s"
					.formatted(profile.slug(), service.id(), service.slug(), mergeOutcome));
		}

		Assert.state(mergeOutcome.isApplied(), "Unexpected outcome when attempting to merge changeset: " + mergeOutcome);
		Assert.state(mergeOutcome.revision() != null, "Merge outcome revision must not be null");
		final Map<String, PropertyHistory> history = new LinkedHashMap<>(changes.size());

		for (PropertyChange change : changes) {
			final PropertyHistory diff = switch (change.operation()) {
				case CREATE -> PropertyHistory.added(mergeOutcome.author(), change.value(), mergeOutcome.timestamp());
				case MODIFY -> PropertyHistory.updated(mergeOutcome.author(), change.value(),
						unseal(current, change.name(), keysetOperations), mergeOutcome.timestamp());
				case REMOVE -> PropertyHistory.removed(mergeOutcome.author(), unseal(current, change.name(), keysetOperations),
						mergeOutcome.timestamp());
			};

			history.put(change.name(), diff);
		}

		// update the new state of the vault
		state = updated;

		return new ApplyResult(mergeOutcome.revision(), Collections.unmodifiableMap(history), mergeOutcome.timestamp());
	}

	@NonNull
	@Override
	public Vault submit(@NonNull PropertyChanges changes) {
		if (profile.policy() == ProfilePolicy.IMMUTABLE) {
			throw ProfilePolicyViolationException.immutableProfile(profile);
		}
		throw new UnsupportedOperationException("Submitting changes is not yet supported.");
	}

	@Override
	public void close() throws Exception {
		repository.close();
	}

	private static String unseal(Properties properties, String name, KeysetOperations operations) {
		return properties.get(name)
				.map(value -> unseal(value, operations))
				.orElseThrow(() -> new IllegalArgumentException("Failed to find property in state with name: " + name));
	}

	private static String unseal(PropertyValue value, KeysetOperations operations) {
		final PropertyValue unsealed = value.unseal(operations);
		return new String(unsealed.get().array(), StandardCharsets.UTF_8);
	}
}
