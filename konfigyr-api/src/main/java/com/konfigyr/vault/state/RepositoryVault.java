package com.konfigyr.vault.state;


import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.namespace.Service;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.vault.*;
import com.konfigyr.vault.Properties;
import com.konfigyr.vault.changes.ChangeRequestCreateCommand;
import com.konfigyr.vault.changes.ChangeRequestManager;
import com.konfigyr.vault.changes.ChangeRequestRevision;
import com.konfigyr.vault.changes.ChangeRequestUpdateCommand;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.OrderedMapIterator;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Builder
final class RepositoryVault implements Vault {

	private final Service service;
	private final Profile profile;
	private final AuthenticatedPrincipal author;
	private final StateRepository stateRepository;
	private final ChangeRequestManager changeRequestManager;
	private final KeysetOperations keysetOperations;

	private volatile Revision revision;

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
		return revision().properties();
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

		final MergeOutcome updateOutcome = stateRepository.update(profile, changeset);

		if (!updateOutcome.isApplied()) {
			stateRepository.discard(profile, updateOutcome.branch());

			throw new IllegalStateException("Failed to prepare changeset for profile '%s' of Service(%s, %s) due to: %s"
					.formatted(profile.slug(), service.id(), service.slug(), updateOutcome));
		}

		final MergeOutcome mergeOutcome = stateRepository.merge(profile, updateOutcome.branch());

		if (mergeOutcome.isConflicting()) {
			stateRepository.discard(profile, updateOutcome.branch());

			Assert.state(mergeOutcome.conflicts() != null, "Merge conflicts must not be null");
			throw new ConflictingProfileStateException(profile, mergeOutcome.conflicts());
		} else if (mergeOutcome.isUnknown()) {
			stateRepository.discard(profile, updateOutcome.branch());

			throw new IllegalStateException("Failed to apply changes to profile '%s' of Service(%s, %s) due to: %s"
					.formatted(profile.slug(), service.id(), service.slug(), mergeOutcome));
		}

		Assert.state(mergeOutcome.isApplied(), "Unexpected outcome when attempting to merge changeset: " + mergeOutcome);

		final ApplyResult result = createChangeResult(mergeOutcome, changes, current, updated);

		// update the new state of the vault...
		revision = new Revision(result.revision(), updated);

		return result;
	}

	@NonNull
	@Override
	public ChangeRequest submit(@NonNull PropertyChanges changes) {
		if (profile.policy() == ProfilePolicy.IMMUTABLE) {
			throw ProfilePolicyViolationException.immutableProfile(profile);
		}

		final Properties updated = state().apply(changes, keysetOperations);
		final Changeset changeset = new Changeset(author, updated, changes);

		final MergeOutcome updateOutcome = stateRepository.update(profile, changeset);

		if (!updateOutcome.isApplied()) {
			stateRepository.discard(profile, updateOutcome.branch());

			throw new IllegalStateException("Failed to prepare changeset for profile '%s' of Service(%s, %s) due to: %s"
					.formatted(profile.slug(), service.id(), service.slug(), updateOutcome));
		}

		final ApplyResult result = createChangeResult(updateOutcome, changes, state(), updated);

		try {
			return changeRequestManager.create(new ChangeRequestCreateCommand(
					service, profile, result, updateOutcome.branch()
			));
		} catch (Exception ex) {
			stateRepository.discard(profile, updateOutcome.branch());
			throw ex;
		}
	}

	@NonNull
	@Override
	public ApplyResult merge(@NonNull ChangeRequest changeRequest) {
		Assert.state(
				changeRequest.state() == ChangeRequestState.OPEN,
				() -> "ChangeRequest(id=%s, state=%s) must be in open state to be merged".formatted(
						changeRequest.id(), changeRequest.state()
				)
		);

		final ChangeRequestRevision changeRequestRevision = changeRequestManager.revision(changeRequest);
		final Revision headRevision = revision();

		// TODO: check revision states before merging...

		final MergeOutcome mergeOutcome = stateRepository.merge(profile, changeRequestRevision.branch());

		if (mergeOutcome.isConflicting()) {
			Assert.state(mergeOutcome.conflicts() != null, "Merge conflicts must not be null");
			throw new ConflictingProfileStateException(profile, mergeOutcome.conflicts());
		} else if (mergeOutcome.isUnknown()) {
			throw new IllegalStateException("Failed to apply changes to profile '%s' of Service(%s, %s) due to: %s"
					.formatted(profile.slug(), service.id(), service.slug(), mergeOutcome));
		}

		Assert.state(mergeOutcome.revision() != null, "Merge outcome revision must not be null");
		Assert.state(mergeOutcome.isApplied(), "Unexpected outcome when attempting to merge changeset: " + mergeOutcome);

		// force the update of the new state of the vault...
		revision = null;

		changeRequestManager.update(new ChangeRequestUpdateCommand(changeRequest, author, ChangeRequestState.MERGED));

		return new ApplyResult(
				mergeOutcome.revision(),
				headRevision.revision(),
				changeRequest.subject(),
				changeRequest.description() == null ? null : changeRequest.description().value(),
				Set.copyOf(changeRequestRevision.changes()),
				author,
				mergeOutcome.timestamp()
		);
	}

	@NonNull
	@Override
	public ChangeRequest discard(@NonNull ChangeRequest changeRequest) {
		Assert.state(
				changeRequest.state() == ChangeRequestState.OPEN,
				() -> "ChangeRequest(id=%s, state=%s) must be in open state to be discarded".formatted(
						changeRequest.id(), changeRequest.state()
				)
		);

		final ChangeRequestRevision revision = changeRequestManager.revision(changeRequest);

		try {
			stateRepository.discard(profile, revision.branch());
		} catch (RepositoryStateException ex) {
			if (ex.getErrorCode() == RepositoryStateException.ErrorCode.UNKNOWN_CHANGESET) {
				log.info("Changeset branch {} was already discarded for ChangeRequest({})", revision.branch(), changeRequest.id());
			} else {
				throw ex;
			}
		}

		return changeRequestManager.update(new ChangeRequestUpdateCommand(changeRequest, author, ChangeRequestState.DISCARDED));
	}

	@Override
	public void close() throws Exception {
		stateRepository.close();
	}

	private ApplyResult createChangeResult(MergeOutcome outcome, PropertyChanges changes, Properties current, Properties next) {
		Assert.state(outcome.revision() != null, "Merge outcome revision must not be null");

		final SortedSet<PropertyTransition> transitions = new TreeSet<>(PropertyTransition::compareTo);

		for (PropertyChange change : changes) {
			final PropertyTransition transition = switch (change.operation()) {
				case CREATE -> PropertyTransition.added(
						change.name(),
						get(next, change.name())
				);
				case MODIFY -> PropertyTransition.updated(
						change.name(),
						get(current, change.name()),
						get(next, change.name())
				);
				case REMOVE -> PropertyTransition.removed(
						change.name(),
						get(current, change.name())
				);
			};
			transitions.add(transition);
		}

		return new ApplyResult(
				outcome.revision(),
				revision().revision(),
				changes.subject(),
				changes.description(),
				Collections.unmodifiableSet(transitions),
				author,
				outcome.timestamp()
		);
	}

	private Revision revision() {
		if (revision == null) {
			try {
				final RepositoryState repositoryState = stateRepository.get(profile);
				revision = new Revision(repositoryState.revision(), Properties.from(repositoryState));
			} catch (IOException ex) {
				throw new RepositoryStateException(RepositoryStateException.ErrorCode.CORRUPTED_STATE,
						"Failed read repository configuration state for '%s' profile of Service(%s, %s)".formatted(
								profile.slug(), service.id(), service.slug()), ex);
			}
		}
		return revision;
	}

	private static PropertyValue get(Properties properties, String name) {
		final PropertyValue value = properties.get(name)
				.orElseThrow(() -> new IllegalStateException("Failed to find property in state with name: " + name));
		Assert.state(value.isSealed(), "Property value must be sealed");
		return value;
	}

	private record Revision(String revision, Properties properties) implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;
	}
}
