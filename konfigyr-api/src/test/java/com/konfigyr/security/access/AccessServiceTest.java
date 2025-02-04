package com.konfigyr.security.access;

import com.konfigyr.account.Account;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.test.TestAccounts;
import com.konfigyr.test.TestPrincipals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AccessServiceTest {

	final Account account = TestAccounts.john().build();
	final Authentication authentication = TestPrincipals.from(account);
	final ObjectIdentity objectIdentity = ObjectIdentity.namespace("konfigyr");

	@Mock
	AccessControlRepository repository;

	Cache cache;

	AccessService service;

	@BeforeEach
	void setup() {
		cache = spy(new ConcurrentMapCache("test-cache"));
		service = new KonfigyrAccessService(repository, cache);
	}

	@Test
	@DisplayName("should fail to retrieve access control from repository")
	void repositoryFails() {
		doThrow(IllegalArgumentException.class).when(repository).get(objectIdentity);

		assertThatExceptionOfType(AccessControlException.class)
				.isThrownBy(() -> service.hasAccess(authentication, "konfigyr"))
				.withMessageContaining("Failed to retrieve access control")
				.withCauseInstanceOf(IllegalArgumentException.class);

		verify(repository).get(objectIdentity);
		verify(cache).get(objectIdentity);
		verify(cache, never()).put(eq(objectIdentity), any());
	}

	@Test
	@DisplayName("should grant access when access control is not found")
	void accessControlNotFound() {
		doThrow(ObjectIdentityNotFound.class).when(repository).get(objectIdentity);

		assertThat(service.hasAccess(authentication, "konfigyr")).isTrue();

		verify(repository).get(objectIdentity);
		verify(cache).get(objectIdentity);
		verify(cache).put(objectIdentity, null);
	}

	@Test
	@DisplayName("should not grant access")
	void accessControlNotGranted() {
		final var control = accessControlFor(accessGrantFor(account, NamespaceRole.USER));
		doReturn(control).when(repository).get(objectIdentity);

		assertThat(service.hasAccess(authentication, "konfigyr", NamespaceRole.ADMIN)).isFalse();

		verify(repository).get(objectIdentity);
		verify(cache).get(objectIdentity);
		verify(cache).put(objectIdentity, control);
	}

	@Test
	@DisplayName("should grant access for namespace user")
	void accessControlGrantedForUser() {
		final var control = accessControlFor(accessGrantFor(account, NamespaceRole.USER));
		doReturn(control).when(repository).get(objectIdentity);

		assertThat(service.hasAccess(authentication, "konfigyr")).isTrue();

		verify(repository).get(objectIdentity);
		verify(cache).get(objectIdentity);
		verify(cache).put(objectIdentity, control);
	}

	@Test
	@DisplayName("should grant access for namespace administrator")
	void accessControlGrantedForAdmin() {
		final var control = accessControlFor(accessGrantFor(account, NamespaceRole.ADMIN));
		doReturn(control).when(repository).get(objectIdentity);

		assertThat(service.hasAccess(authentication, "konfigyr", NamespaceRole.ADMIN)).isTrue();

		verify(repository).get(objectIdentity);
		verify(cache).get(objectIdentity);
		verify(cache).put(objectIdentity, control);
	}

	@Test
	@DisplayName("should not grant access for unsupported authentication")
	void accessControlForUnsupportedAuthentication() {
		final var authentication = new TestingAuthenticationToken("test", "test");

		assertThat(service.hasAccess(authentication, "konfigyr", NamespaceRole.ADMIN)).isFalse();

		verifyNoInteractions(repository);
		verifyNoInteractions(cache);
	}

	@Test
	@DisplayName("should cache access controls for object identities")
	void accessControlCache() {
		final var control = accessControlFor(accessGrantFor(account, NamespaceRole.ADMIN));

		doThrow(ObjectIdentityNotFound.class).when(repository).get(ObjectIdentity.namespace("john-doe"));
		doReturn(control).when(repository).get(objectIdentity);

		assertThat(service.hasAccess(authentication, "konfigyr", NamespaceRole.ADMIN)).isTrue();
		assertThat(service.hasAccess(authentication, "konfigyr", NamespaceRole.USER)).isFalse();

		assertThat(service.hasAccess(authentication, "john-doe", NamespaceRole.ADMIN)).isTrue();
		assertThat(service.hasAccess(authentication, "john-doe", NamespaceRole.USER)).isTrue();

		assertThat(cache.get(objectIdentity, AccessControl.class))
				.isEqualTo(control);

		assertThat(cache.get(ObjectIdentity.namespace("john-doe"), AccessControl.class))
				.isNull();

		verify(repository).get(objectIdentity);
		verify(repository).get(ObjectIdentity.namespace("john-doe"));
	}

	static Collection<AccessGrant> accessGrantFor(Account account, NamespaceRole... roles) {
		return Arrays.stream(roles)
				.map(role -> AccessGrant.of(account.id(), role))
				.collect(Collectors.toSet());
	}

	static AccessControl accessControlFor(Collection<AccessGrant> grants) {
		return KonfigyrAccessControl.namespace(EntityId.from(1), grants);
	}

}
