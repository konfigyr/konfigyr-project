package com.konfigyr.vault.extension;

import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.namespace.Service;
import com.konfigyr.vault.*;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObservedVaultExtensionTest {

	@Mock
	Vault vault;

	@Mock
	Service service;

	@Mock
	Profile profile;

	TestObservationRegistry observationRegistry;

	ObservedVaultExtension extension;

	@BeforeEach
	void setup() {
		doReturn(EntityId.from(1000)).when(service).id();
		doReturn(EntityId.from(2000)).when(profile).id();
		doReturn(service).when(vault).service();
		doReturn(profile).when(vault).profile();

		observationRegistry = TestObservationRegistry.create();
		extension = new ObservedVaultExtension(observationRegistry);
	}

	@Test
	@DisplayName("should observe retrieving current configuration state")
	void shouldObserveStateOperation() {
		final var properties = Properties.builder().build();
		doReturn(properties).when(vault).state();

		final var extended = extension.extend(vault);

		assertThat(extended.state())
				.isSameAs(properties);

		verify(vault).state();

		assertObservation(ObservedVaultExtension.ObservedOperation.STATE)
				.doesNotHaveError();
	}

	@Test
	@DisplayName("should observe unsealing vault configuration state")
	void shouldObserveUnsealOperation() {
		final var properties = Map.of("spring.application.name", "konfigyr");
		doReturn(properties).when(vault).unseal();

		final var extended = extension.extend(vault);

		assertThat(extended.unseal())
				.isSameAs(properties);

		verify(vault).unseal();

		assertObservation(ObservedVaultExtension.ObservedOperation.UNSEAL)
				.doesNotHaveError();
	}

	@Test
	@DisplayName("should observe sealing a property value using the vault encryption keyset")
	void shouldObserveSealPropertyOperation() {
		final var sealed = PropertyValue.sealed(ByteArray.fromString("sealed"), ByteArray.fromString("checksum"));
		final var unsealed = PropertyValue.unsealed(ByteArray.fromString("unsealed"), ByteArray.fromString("checksum"));
		doReturn(sealed).when(vault).seal(unsealed);

		final var extended = extension.extend(vault);

		assertThat(extended.seal(unsealed))
				.isSameAs(sealed);

		verify(vault).seal(unsealed);

		assertObservation(ObservedVaultExtension.ObservedOperation.SEAL_PROPERTY)
				.doesNotHaveError();
	}

	@Test
	@DisplayName("should observe unsealing a property value using the vault encryption keyset")
	void shouldObserveUnsealPropertyOperation() {
		final var sealed = PropertyValue.sealed(ByteArray.fromString("sealed"), ByteArray.fromString("checksum"));
		final var unsealed = PropertyValue.unsealed(ByteArray.fromString("unsealed"), ByteArray.fromString("checksum"));
		doReturn(unsealed).when(vault).unseal(sealed);

		final var extended = extension.extend(vault);

		assertThat(extended.unseal(sealed))
				.isSameAs(unsealed);

		verify(vault).unseal(sealed);

		assertObservation(ObservedVaultExtension.ObservedOperation.UNSEAL_PROPERTY)
				.doesNotHaveError();
	}

	@Test
	@DisplayName("should observe applying changes to the vault profile")
	void shouldObserveApplyOperation() {
		final var changes = mock(PropertyChanges.class);
		final var result = mock(ApplyResult.class);
		doReturn(result).when(vault).apply(changes);

		final var extended = extension.extend(vault);

		assertThat(extended.apply(changes))
				.isSameAs(result);

		verify(vault).apply(changes);

		assertObservation(ObservedVaultExtension.ObservedOperation.APPLY)
				.doesNotHaveError();
	}

	@Test
	@DisplayName("should observe submitting changes to the vault profile")
	void shouldObserveSubmitOperation() {
		final var changes = mock(PropertyChanges.class);
		final var request = mock(ChangeRequest.class);
		doReturn(request).when(vault).submit(changes);

		final var extended = extension.extend(vault);

		assertThat(extended.submit(changes))
				.isSameAs(request);

		verify(vault).submit(changes);

		assertObservation(ObservedVaultExtension.ObservedOperation.SUBMIT)
				.doesNotHaveError();
	}

	@Test
	@DisplayName("should observe merging proposed changes to the vault profile")
	void shouldObserveMergeOperation() {
		final var request = mock(ChangeRequest.class);
		final var result = mock(ApplyResult.class);
		doReturn(EntityId.from(4000)).when(request).id();
		doReturn(result).when(vault).merge(request);

		final var extended = extension.extend(vault);

		assertThat(extended.merge(request))
				.isSameAs(result);

		verify(vault).merge(request);

		assertObservation(ObservedVaultExtension.ObservedOperation.MERGE)
				.hasHighCardinalityKeyValue("konfigyr.vault.change-request", EntityId.from(4000).serialize())
				.doesNotHaveError();
	}

	@Test
	@DisplayName("should observe discarding proposed changes")
	void shouldObserveDiscardOperation() {
		final var request = mock(ChangeRequest.class);
		final var discarded = mock(ChangeRequest.class);
		doReturn(EntityId.from(5000)).when(request).id();
		doReturn(discarded).when(vault).discard(request);

		final var extended = extension.extend(vault);

		assertThat(extended.discard(request))
				.isSameAs(discarded);

		verify(vault).discard(request);

		assertObservation(ObservedVaultExtension.ObservedOperation.DISCARD)
				.hasHighCardinalityKeyValue("konfigyr.vault.change-request", EntityId.from(5000).serialize())
				.doesNotHaveError();
	}

	@Test
	@DisplayName("should propagate exceptions from the delegate vault and record them in the observation")
	void shouldPropagateExceptions() throws Exception {
		final var cause = new IllegalStateException("boom");
		doThrow(cause).when(vault).close();

		final var extended = extension.extend(vault);

		assertThatIllegalStateException()
				.isThrownBy(extended::close)
				.isEqualTo(cause);

		verify(vault).close();

		assertObservation(ObservedVaultExtension.ObservedOperation.CLOSE)
				.hasError(cause);
	}

	TestObservationRegistryAssert.TestObservationRegistryAssertReturningObservationContextAssert assertObservation(
			ObservedVaultExtension.ObservedOperation operation
	) {
		return assertThat(observationRegistry)
				.hasObservationWithNameEqualTo(operation.observationName())
				.that()
				.hasBeenStarted()
				.hasBeenStopped()
				.hasContextualNameEqualTo(operation.contextualName(EntityId.from(1000).serialize(), EntityId.from(2000).serialize()))
				.hasHighCardinalityKeyValue("konfigyr.namespace.service", EntityId.from(1000).serialize())
				.hasHighCardinalityKeyValue("konfigyr.vault.profile", EntityId.from(2000).serialize());
	}

}
