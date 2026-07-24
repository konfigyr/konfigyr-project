package com.konfigyr.vault.history;

import com.konfigyr.vault.ApplyResult;
import com.konfigyr.vault.Profile;
import com.konfigyr.vault.ProfilePolicy;
import com.konfigyr.vault.VaultEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChangeHistoryListenerTest {

	static Profile profile = Profile.builder()
			.id(45867635L)
			.service(145682578)
			.slug("test-profile")
			.name("Test profile")
			.policy(ProfilePolicy.IMMUTABLE)
			.build();

	@Mock
	ChangeHistoryService service;

	@Mock
	ApplyResult result;

	ChangeHistoryListener listener;

	@BeforeEach
	void setup() {
		listener = new ChangeHistoryListener(service);
	}

	@Test
	@DisplayName("should commit the applied changes for the profile to build the property change history")
	void commitAppliedChanges() {
		final var event = new VaultEvent.ChangesApplied(profile, result);
		assertThatNoException().isThrownBy(() -> listener.createChangeHistory(event));

		verify(service).commit(profile.id(), result);
	}

	@Test
	@DisplayName("should synchronize the applied changes for the profile to build the property index")
	void syncAppliedChanges() {
		final var event = new VaultEvent.ChangesApplied(profile, result);
		assertThatNoException().isThrownBy(() -> listener.syncProperties(event));

		verify(service).synchronize(profile.id(), result);
	}
}
