package com.konfigyr.identity.authorization.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DelegatingRegisteredClientRepositoryTest {

	@Mock
	RegisteredClientRepository first;

	@Mock
	RegisteredClientRepository second;

	@Mock
	RegisteredClient client;

	RegisteredClientRepository repository;

	@BeforeEach
	void setup() {
		repository = new DelegatingRegisteredClientRepository(List.of(first, second));
	}

	@Test
	@DisplayName("should throw operation not supported when trying to save client")
	void shouldNotStoreClients() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> repository.save(client))
				.withMessageContaining("Registering OAuth clients is not supported")
				.withNoCause();
	}

	@Test
	@DisplayName("should retrieve a registered client by client_id from the first repository")
	void shouldDelegateToFirstRepository() {
		doReturn(client).when(first).findByClientId(anyString());

		assertThat(repository.findByClientId("client_id"))
				.isSameAs(client);

		verify(first).findByClientId("client_id");
		verifyNoInteractions(second);
	}

	@Test
	@DisplayName("should retrieve a registered client by registration identifier from the second repository")
	void shouldDelegateToSecondRepository() {
		doReturn(client).when(second).findById(anyString());

		assertThat(repository.findById("registration_id"))
				.isSameAs(client);

		verify(first).findById("registration_id");
		verify(second).findById("registration_id");
	}

	@Test
	@DisplayName("should not lookup client for empty client_id or client registration identifiers")
	void emptyIdentifiers() {
		assertThat(repository.findByClientId(""))
				.isNull();

		assertThat(repository.findById("  "))
				.isNull();

		verifyNoInteractions(first);
		verifyNoInteractions(second);
	}

	@Test
	@DisplayName("should fail to retrieve a registered client by client_id when none of the delegates contain the client")
	void emptyForClientId() {
		assertThat(repository.findByClientId("client_id"))
				.isNull();

		verify(first).findByClientId("client_id");
		verify(second).findByClientId("client_id");
	}

	@Test
	@DisplayName("should fail to retrieve a registered client by registration id when none of the delegates contain the client")
	void emptyForRegistrationId() {
		assertThat(repository.findById("registration_id"))
				.isNull();

		verify(first).findById("registration_id");
		verify(second).findById("registration_id");
	}

	@Test
	@DisplayName("should rethrow authentication exceptions thrown by delegates")
	void rethrowAuthenticationExceptions() {
		final var cause = new CredentialsExpiredException("error");
		doThrow(cause).when(first).findByClientId(anyString());

		assertThatException()
				.isThrownBy(() -> repository.findByClientId("client_id"))
				.isEqualTo(cause);

		verify(first).findByClientId("client_id");
		verify(second).findByClientId("client_id");
	}

	@Test
	@DisplayName("should rethrow unexpected exceptions thrown by delegates")
	void wrapDelegateRepositoryExceptions() {
		final var cause = new IllegalArgumentException("error");
		doThrow(cause).when(second).findByClientId(anyString());

		assertThatExceptionOfType(AuthenticationException.class)
				.isThrownBy(() -> repository.findByClientId("client_id"))
				.withMessageContaining("Failed to lookup registered OAuth client")
				.withCause(cause);

		verify(first).findByClientId("client_id");
		verify(second).findByClientId("client_id");
	}
}
