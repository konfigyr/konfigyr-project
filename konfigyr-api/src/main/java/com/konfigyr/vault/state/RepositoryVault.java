package com.konfigyr.vault.state;


import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.namespace.Service;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.vault.*;
import com.konfigyr.vault.Properties;
import lombok.Builder;
import org.apache.commons.collections4.OrderedMapIterator;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Builder
final class RepositoryVault implements Vault {

	private final Service service;
	private final Profile profile;
	private final AuthenticatedPrincipal author;
	private final StateRepository repository;
	private final KeysetOperations keysetOperations;

	private volatile PropertiesState state;

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

	@NonNull
	@Override
	public PropertyValue seal(@NonNull PropertyValue property) {
		return property.seal(keysetOperations);
	}

	@NonNull
	@Override
	public PropertyValue unseal(@NonNull PropertyValue property) {
		return property.unseal(keysetOperations);
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
				final RepositoryState repositoryState = repository.get(profile);
				state = new PropertiesState(repositoryState.revision(), Properties.from(repositoryState));
			} catch (IOException ex) {
				throw new RepositoryStateException(RepositoryStateException.ErrorCode.CORRUPTED_STATE,
						"Failed read repository configuration state for '%s' profile of Service(%s, %s)".formatted(
								profile.slug(), service.id(), service.slug()), ex);
			}
		}
		return state.properties();
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
		final SortedSet<PropertyTransition> transitions = new TreeSet<>(PropertyTransition::compareTo);

		for (PropertyChange change : changes) {
			final PropertyTransition transition = switch (change.operation()) {
				case CREATE -> PropertyTransition.added(
						change.name(),
						get(updated, change.name())
				);
				case MODIFY -> PropertyTransition.updated(
						change.name(),
						get(current, change.name()),
						get(updated, change.name())
				);
				case REMOVE -> PropertyTransition.removed(
						change.name(),
						get(current, change.name())
				);
			};
			transitions.add(transition);
		}

		// grab the previous revision from the state and update it
		// to the new state of the vault using the merge outcome
		final String parentRevision = state.revision();
		state = new PropertiesState(mergeOutcome.revision(), updated);

		return new ApplyResult(
				mergeOutcome.revision(),
				parentRevision,
				changes.subject(),
				changes.description(),
				Collections.unmodifiableSet(transitions),
				author,
				mergeOutcome.timestamp()
		);
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

	private static PropertyValue get(Properties properties, String name) {
		final PropertyValue value = properties.get(name)
				.orElseThrow(() -> new IllegalStateException("Failed to find property in state with name: " + name));
		Assert.state(value.isSealed(), "Property value must be sealed");
		return value;
	}

	private record PropertiesState(String revision, Properties properties) implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;
	}
}
