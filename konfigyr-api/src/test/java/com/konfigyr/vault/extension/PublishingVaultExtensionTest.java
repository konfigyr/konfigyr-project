package com.konfigyr.vault.extension;

import com.konfigyr.entity.EntityId;
import com.konfigyr.vault.*;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublishingVaultExtensionTest {

	@Captor
	ArgumentCaptor<Object> captor;

	@Mock
	ApplicationEventPublisher eventPublisher;

	@Mock
	ChangeRequest changeRequest;

	@Mock
	PropertyChanges changes;

	@Mock
	Profile profile;

	@Mock
	Vault vault;

	PublishingVaultExtension extension;

	@BeforeEach
	void setup() {
		extension = new PublishingVaultExtension(eventPublisher);
	}

	@Test
	@DisplayName("should publish vault changes applied event when property changes are successfully applied")
	void publishChangesAppliedEvent() {
		final var result = mock(ApplyResult.class);

		doReturn(result).when(vault).apply(changes);
		doReturn(profile).when(vault).profile();

		doReturn(EntityId.from(57153)).when(profile).id();

		final var extended = extension.extend(vault);

		assertThat(extended.apply(changes))
				.isEqualTo(result);

		verify(vault).profile();
		verify(vault).apply(changes);
		verify(eventPublisher).publishEvent(captor.capture());

		assertThat(captor.getValue())
				.isInstanceOf(VaultEvent.ChangesApplied.class)
				.asInstanceOf(InstanceOfAssertFactories.type(VaultEvent.ChangesApplied.class))
				.returns(EntityId.from(57153), VaultEvent.ChangesApplied::id)
				.returns(result, VaultEvent.ChangesApplied::result);
	}

	@Test
	@DisplayName("should not publish vault changes applied event when property changes can not be applied")
	void doNotPublishChangesAppliedEventWhenCannotApplyChanges() {
		final var cause = new IllegalArgumentException("Test exception");
		doThrow(cause).when(vault).apply(changes);

		final var extended = extension.extend(vault);

		assertThatException()
				.isThrownBy(() -> extended.apply(changes))
				.isEqualTo(cause);

		verify(vault).apply(changes);
		verifyNoInteractions(eventPublisher);
	}

	@Test
	@DisplayName("should publish change request opened event when changes are successfully submitted")
	void publishChangeRequestOpenedEvent() {
		doReturn(changeRequest).when(vault).submit(changes);

		doReturn(EntityId.from(6826)).when(changeRequest).id();

		final var extended = extension.extend(vault);

		assertThat(extended.submit(changes))
				.isEqualTo(changeRequest);

		verify(vault).submit(changes);
		verify(eventPublisher).publishEvent(captor.capture());

		assertThat(captor.getValue())
				.isInstanceOf(ChangeRequestEvent.Opened.class)
				.asInstanceOf(InstanceOfAssertFactories.type(ChangeRequestEvent.Opened.class))
				.returns(EntityId.from(6826), ChangeRequestEvent.Opened::id);
	}

	@Test
	@DisplayName("should not publish any events when changes can not be submitted")
	void doNotPublishEventsWhenCannotSubmitChanges() {
		final var cause = new IllegalArgumentException("Test exception");
		doThrow(cause).when(vault).submit(changes);

		final var extended = extension.extend(vault);

		assertThatException()
				.isThrownBy(() -> extended.submit(changes))
				.isEqualTo(cause);

		verify(vault).submit(changes);
		verifyNoInteractions(eventPublisher);
	}

	@Test
	@DisplayName("should publish vault changes applied and change request merged event when merging change request")
	void publishChangeRequestMerged() {
		final var result = mock(ApplyResult.class);

		doReturn(result).when(vault).merge(changeRequest);
		doReturn(profile).when(vault).profile();

		doReturn(EntityId.from(1625)).when(changeRequest).id();
		doReturn(EntityId.from(57153)).when(profile).id();

		final var extended = extension.extend(vault);

		assertThat(extended.merge(changeRequest))
				.isEqualTo(result);

		verify(vault).profile();
		verify(vault).merge(changeRequest);
		verify(eventPublisher, times(2)).publishEvent(captor.capture());

		assertThat(captor.getAllValues())
				.hasSize(2)
				.satisfiesExactly(
						event -> assertThat(event)
								.isInstanceOf(VaultEvent.ChangesApplied.class)
								.asInstanceOf(InstanceOfAssertFactories.type(VaultEvent.ChangesApplied.class))
								.returns(EntityId.from(57153), VaultEvent.ChangesApplied::id)
								.returns(result, VaultEvent.ChangesApplied::result),
						event -> assertThat(event)
								.isInstanceOf(ChangeRequestEvent.Merged.class)
								.asInstanceOf(InstanceOfAssertFactories.type(ChangeRequestEvent.Merged.class))
								.returns(EntityId.from(1625), ChangeRequestEvent.Merged::id)
								.returns(result, ChangeRequestEvent.Merged::result)
				);
	}

	@Test
	@DisplayName("should not publish any events when change request can not be merged")
	void doNotPublishEventsWhenCannotMergeChangeRequest() {
		final var cause = new IllegalArgumentException("Test exception");
		doThrow(cause).when(vault).merge(changeRequest);

		final var extended = extension.extend(vault);

		assertThatException()
				.isThrownBy(() -> extended.merge(changeRequest))
				.isEqualTo(cause);

		verify(vault).merge(changeRequest);
		verifyNoInteractions(eventPublisher);
	}

	@Test
	@DisplayName("should publish change request discarded event when change request is successfully discarded")
	void publishChangeRequestDiscardedEvent() {
		final var discarded = mock(ChangeRequest.class);
		doReturn(discarded).when(vault).discard(changeRequest);

		doReturn(EntityId.from(5736)).when(discarded).id();

		final var extended = extension.extend(vault);

		assertThat(extended.discard(changeRequest))
				.isEqualTo(discarded);

		verify(vault).discard(changeRequest);
		verify(eventPublisher).publishEvent(captor.capture());

		assertThat(captor.getValue())
				.isInstanceOf(ChangeRequestEvent.Discarded.class)
				.asInstanceOf(InstanceOfAssertFactories.type(ChangeRequestEvent.Discarded.class))
				.returns(EntityId.from(5736), ChangeRequestEvent.Discarded::id);
	}

	@Test
	@DisplayName("should not publish any events when change request can not be discarded")
	void doNotPublishEventsWhenCannotDiscardChangeRequest() {
		final var cause = new IllegalArgumentException("Test exception");
		doThrow(cause).when(vault).discard(changeRequest);

		final var extended = extension.extend(vault);

		assertThatException()
				.isThrownBy(() -> extended.discard(changeRequest))
				.isEqualTo(cause);

		verify(vault).discard(changeRequest);
		verifyNoInteractions(eventPublisher);
	}

}
