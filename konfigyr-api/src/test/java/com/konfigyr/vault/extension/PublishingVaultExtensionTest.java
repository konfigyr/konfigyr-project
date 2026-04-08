package com.konfigyr.vault.extension;

import com.konfigyr.entity.EntityId;
import com.konfigyr.vault.*;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublishingVaultExtensionTest {

	@Mock
	ApplicationEventPublisher eventPublisher;

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
		verify(eventPublisher).publishEvent(assertArg((Object event) -> assertThat(event)
				.isInstanceOf(VaultEvent.ChangesApplied.class)
				.asInstanceOf(InstanceOfAssertFactories.type(VaultEvent.ChangesApplied.class))
				.returns(EntityId.from(57153), VaultEvent.ChangesApplied::id)
				.returns(result, VaultEvent.ChangesApplied::result)
		));
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

}
