package com.konfigyr.identity.authorization.issuer;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompositeTrustedIssuerRepositoryTest {

	static final EntityId NAMESPACE = EntityId.from(5L);
	static final String ISSUER_URI = "https://token.test.example.com";

	static final TrustedIssuer TRUSTED_ISSUER = new TrustedIssuer(ISSUER_URI, "Test", null, Set.of());

	@Mock
	TrustedIssuerRepository first;

	@Mock
	TrustedIssuerRepository second;

	@Mock
	TrustedIssuerRepository third;

	@Test
	@DisplayName("returns null when no delegates are registered")
	void returnsNullWithNoDelegates() {
		final var composite = new CompositeTrustedIssuerRepository(List.of());

		assertThat(composite.lookup(NAMESPACE, ISSUER_URI)).isNull();
	}

	@Test
	@DisplayName("returns the result from the first delegate that resolves the issuer")
	void returnsFirstMatch() {
		doReturn(TRUSTED_ISSUER).when(first).lookup(NAMESPACE, ISSUER_URI);

		final var composite = new CompositeTrustedIssuerRepository(first, second);

		assertThat(composite.lookup(NAMESPACE, ISSUER_URI)).isSameAs(TRUSTED_ISSUER);
	}

	@Test
	@DisplayName("skips delegates that return null and returns the first non-null result")
	void skipsNullAndReturnsSecondMatch() {
		doReturn(null).when(first).lookup(NAMESPACE, ISSUER_URI);
		doReturn(TRUSTED_ISSUER).when(second).lookup(NAMESPACE, ISSUER_URI);

		final var composite = new CompositeTrustedIssuerRepository(first, second, third);

		assertThat(composite.lookup(NAMESPACE, ISSUER_URI)).isSameAs(TRUSTED_ISSUER);
	}

	@Test
	@DisplayName("does not query subsequent delegates once a match is found")
	void stopsAtFirstMatch() {
		doReturn(TRUSTED_ISSUER).when(second).lookup(NAMESPACE, ISSUER_URI);

		new CompositeTrustedIssuerRepository(first, second, third).lookup(NAMESPACE, ISSUER_URI);

		verify(first).lookup(NAMESPACE, ISSUER_URI);
		verify(second).lookup(NAMESPACE, ISSUER_URI);
		verify(third, never()).lookup(NAMESPACE, ISSUER_URI);
	}

	@Test
	@DisplayName("returns null when all delegates return null")
	void returnsNullWhenAllDelegatesMiss() {
		doReturn(null).when(first).lookup(NAMESPACE, ISSUER_URI);
		doReturn(null).when(second).lookup(NAMESPACE, ISSUER_URI);

		final var composite = new CompositeTrustedIssuerRepository(first, second);

		assertThat(composite.lookup(NAMESPACE, ISSUER_URI)).isNull();

		verify(first).lookup(NAMESPACE, ISSUER_URI);
		verify(second).lookup(NAMESPACE, ISSUER_URI);
	}

	@Test
	@DisplayName("passes namespace and issuer URI unchanged to each delegate")
	void forwardsArgumentsToDelegate() {
		final var ns = EntityId.from(42L);
		final var uri = "https://specific.issuer.example.com";

		new CompositeTrustedIssuerRepository(first).lookup(ns, uri);

		verify(first).lookup(ns, uri);
	}

}
