package com.konfigyr.vault.extension;

import com.konfigyr.vault.*;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Implementation of {@link VaultExtension} that publishes {@link VaultEvent}s when mutations are
 * successfully applied to the underlying {@link Vault}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@RequiredArgsConstructor
public class PublishingVaultExtension implements VaultExtension {

	private final ApplicationEventPublisher eventPublisher;

	@Override
	public Vault extend(Vault vault) {
		return new PublishingVault(vault, eventPublisher);
	}

	private static final class PublishingVault extends AbstractDelegatingVault {

		private final ApplicationEventPublisher eventPublisher;

		PublishingVault(Vault delegate, ApplicationEventPublisher eventPublisher) {
			super(delegate);
			this.eventPublisher = eventPublisher;
		}

		@Override
		public ApplyResult apply(PropertyChanges changes) {
			final ApplyResult result = super.apply(changes);
			final Profile profile = profile();

			eventPublisher.publishEvent(new VaultEvent.ChangesApplied(profile.id(), result));

			return result;
		}

		@Override
		public ChangeRequest submit(PropertyChanges changes) {
			final ChangeRequest openedChangeRequest = super.submit(changes);

			eventPublisher.publishEvent(new ChangeRequestEvent.Opened(openedChangeRequest.id()));

			return openedChangeRequest;
		}

		@Override
		public ApplyResult merge(ChangeRequest changeRequest) {
			final ApplyResult result = super.merge(changeRequest);
			final Profile profile = profile();

			eventPublisher.publishEvent(new VaultEvent.ChangesApplied(profile.id(), result));
			eventPublisher.publishEvent(new ChangeRequestEvent.Merged(changeRequest.id(), result));

			return result;
		}

		@Override
		public ChangeRequest discard(ChangeRequest changeRequest) {
			final ChangeRequest discaredChangeRequest = super.discard(changeRequest);

			eventPublisher.publishEvent(new ChangeRequestEvent.Discarded(discaredChangeRequest.id()));

			return discaredChangeRequest;
		}
	}
}
