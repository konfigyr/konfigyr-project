package com.konfigyr.identity.authorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationEventsTest {

	@Mock
	OAuth2Authorization authorization;

	@Mock
	OAuth2AuthorizationConsent consent;

	@Test
	@DisplayName("should create authorization stored event")
	void shouldCreateAuthorizationStoredEvent() {
		doReturn("stored").when(authorization).getId();
		final var event = new AuthorizationEvent.Stored(authorization);

		assertThat(event)
				.isNotNull()
				.hasSameHashCodeAs(event)
				.doesNotHaveSameHashCodeAs(new AuthorizationEvent.Stored(authorization))
				.hasToString("AuthorizationStored[id=stored, timestamp=%s]", event.timestamp())
				.returns("stored", AuthorizationEvent::id)
				.satisfies(it -> assertThat(it.timestamp())
						.isCloseTo(Instant.now(), within(200, ChronoUnit.MILLIS))
				);
	}

	@Test
	@DisplayName("should create authorization revoked event")
	void shouldCreateAuthorizationRevokedEvent() {
		final var event = new AuthorizationEvent.Revoked("revoked");

		assertThat(event)
				.isNotNull()
				.hasSameHashCodeAs(event)
				.doesNotHaveSameHashCodeAs(new AuthorizationEvent.Revoked("revoked"))
				.hasToString("AuthorizationRevoked[id=revoked, timestamp=%s]", event.timestamp())
				.returns("revoked", AuthorizationEvent::id)
				.satisfies(it -> assertThat(it.timestamp())
						.isCloseTo(Instant.now(), within(200, ChronoUnit.MILLIS))
				);
	}

	@Test
	@DisplayName("should create authorization consent granted event")
	void shouldCreateAuthorizationConsentGrantedEvent() {
		final var event = new AuthorizationConsentEvent.Granted(consent);

		assertThat(event)
				.isNotNull()
				.hasSameHashCodeAs(event)
				.doesNotHaveSameHashCodeAs(new AuthorizationConsentEvent.Granted(consent))
				.hasToString("AuthorizationConsentGranted[timestamp=%s]", event.timestamp())
				.returns(consent, AuthorizationConsentEvent::consent)
				.satisfies(it -> assertThat(it.timestamp())
						.isCloseTo(Instant.now(), within(200, ChronoUnit.MILLIS))
				);
	}

	@Test
	@DisplayName("should create authorization consent revoked event")
	void shouldCreateAuthorizationConsentRevokedEvent() {
		final var event = new AuthorizationConsentEvent.Revoked(consent);

		assertThat(event)
				.isNotNull()
				.hasSameHashCodeAs(event)
				.doesNotHaveSameHashCodeAs(new AuthorizationConsentEvent.Revoked(consent))
				.hasToString("AuthorizationConsentRevoked[timestamp=%s]", event.timestamp())
				.returns(consent, AuthorizationConsentEvent::consent)
				.satisfies(it -> assertThat(it.timestamp())
						.isCloseTo(Instant.now(), within(200, ChronoUnit.MILLIS))
				);
	}

}
