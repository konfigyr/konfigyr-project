package com.konfigyr.identity.authentication;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.AccountIdentities;
import com.konfigyr.identity.TestClients;
import com.konfigyr.identity.authentication.idenitity.AccountIdentityRepository;
import com.konfigyr.test.OAuth2AccessTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.cache.SpringCacheBasedUserCache;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountIdentityServiceTest {

	final ClientRegistration clientRegistration = TestClients.clientRegistration("konfigyr").build();

	@Mock
	AccountIdentityRepository repository;

	@Mock
	OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

	AccountIdentityService service;

	@BeforeEach
	void setup() {
		service = new DefaultAccountIdentityService(repository);
	}

	@Test
	@DisplayName("should load account identity by identifier")
	void loadByIdentifier() {
		final var account = AccountIdentities.jane().build();

		doReturn(Optional.of(account)).when(repository).findById(account.getId());

		assertThat(service.get(account.getId().serialize()))
				.isNotNull()
				.isEqualTo(account);
	}

	@Test
	@DisplayName("should load account identity by serialized identifier")
	void loadBySerializedIdentifier() {
		final var account = AccountIdentities.john().build();

		doReturn(Optional.of(account)).when(repository).findById(account.getId());

		assertThat(service.get(account.getUsername()))
				.isNotNull()
				.isEqualTo(account);
	}

	@Test
	@DisplayName("should load account identity from cache")
	void loadUserFromCache() {
		final var cache = new ConcurrentMapCache("user-cache");
		service = new DefaultAccountIdentityService(repository, new SpringCacheBasedUserCache(cache));

		final var account = AccountIdentities.john().build();

		doReturn(Optional.of(account)).when(repository).findById(account.getId());

		assertThat(service.get(account.getId().serialize()))
				.isNotNull()
				.isSameAs(service.get(account.getName()))
				.isSameAs(service.get(account.getId().serialize()));

		assertThat(cache.getNativeCache())
				.isNotEmpty()
				.containsEntry(account.getUsername(), account);

		verify(repository).findById(account.getId());
	}

	@Test
	@DisplayName("should evict account identity from cache when invalid")
	void loadEvictInvalidPrincipalFromCache() {
		final var cache = new ConcurrentMapCache("user-cache");
		service = new DefaultAccountIdentityService(repository, new SpringCacheBasedUserCache(cache));

		final var account = AccountIdentities.john().build();

		cache.put(account.getUsername(), User.withUsername("test")
				.password("pass")
				.authorities("testing")
				.build());

		assertThatThrownBy(() -> service.get(account.getUsername()))
				.isInstanceOf(UsernameNotFoundException.class);

		assertThat(cache.getNativeCache())
				.isEmpty();

		verify(repository).findById(account.getId());
	}

	@Test
	@DisplayName("should fail to load account identity by identifier")
	void failToLoadByIdentifier() {
		final var id = EntityId.from(1);

		assertThatThrownBy(() -> service.get(id))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("Failed to lookup user account for identifier:")
				.hasMessageContaining(id.toString())
				.hasNoCause();

		verify(repository).findById(id);
	}

	@Test
	@DisplayName("should fail to load account identity by serialized identifier")
	void failToLoadBySerializedIdentifier() {
		final var id = EntityId.from(1);

		assertThatThrownBy(() -> service.get(id.serialize()))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("Failed to lookup user account for identifier:")
				.hasMessageContaining(id.toString())
				.hasNoCause();

		verify(repository).findById(id);
	}

	@Test
	@DisplayName("should fail load account when identifier is empty, blank or invalid")
	void failToLoadWhenBlankOrInvalid() {
		assertThatThrownBy(() -> service.get(""))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("Failed to lookup user account for invalid username")
				.hasRootCauseInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> service.get("   "))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("Failed to lookup user account for invalid username")
				.hasRootCauseInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> service.get("account@acme.com"))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("Failed to lookup user account for invalid username")
				.hasRootCauseInstanceOf(IllegalArgumentException.class);

		verifyNoInteractions(repository);
	}

	@Test
	@DisplayName("should retrieve existing account identity for OIDC principal")
	void shouldRetrieveExistingIdentity() {
		final var user = mock(OidcUser.class);
		final var account = AccountIdentities.john().build();
		final var request = createOidcRequest(clientRegistration, account);

		doReturn(user).when(delegate).loadUser(request);
		doReturn(account.getEmail()).when(user).getName();
		doReturn(Optional.of(account)).when(repository).findByEmail(account.getEmail());

		assertThat(service.get(delegate, request))
				.isInstanceOf(OidcAccountIdentityUser.class)
				.returns(account, AccountIdentityUser::getAccountIdentity)
				.returns(account.getId(), AccountIdentityUser::getId)
				.returns(account.getName(), AccountIdentityUser::getName);

		verify(repository).findByEmail(account.getEmail());
		verify(repository, never()).create(user, clientRegistration);
	}

	@Test
	@DisplayName("should create account identity for OAuth2 principal")
	void shouldCreateIdentity() {
		final var user = mock(OAuth2User.class);
		final var account = AccountIdentities.john().build();
		final var request = createUserRequest(clientRegistration);

		doReturn(user).when(delegate).loadUser(request);
		doReturn(account.getEmail()).when(user).getName();
		doReturn(account).when(repository).create(user, clientRegistration);

		assertThat(service.get(delegate, request))
				.isInstanceOf(OAuthAccountIdentityUser.class)
				.returns(account, AccountIdentityUser::getAccountIdentity)
				.returns(account.getId(), AccountIdentityUser::getId)
				.returns(account.getName(), AccountIdentityUser::getName);

		verify(repository).findByEmail(account.getEmail());
		verify(repository).create(user, clientRegistration);
	}

	@Test
	@DisplayName("should fail to find OAuth2 user attributes from delegating user service")
	void shouldFailToFindUser() {
		final var request = createUserRequest(clientRegistration);

		assertThatThrownBy(() -> service.get(delegate, request))
				.isInstanceOf(OAuth2AuthenticationException.class);

		verifyNoInteractions(repository);
	}

	@Test
	@DisplayName("should rethrow exceptions thrown by the delegating user service")
	void shouldFailToLoadUser() {
		final var request = createUserRequest(clientRegistration);

		doThrow(OAuth2AuthenticationException.class).when(delegate).loadUser(request);

		assertThatThrownBy(() -> service.get(delegate, request))
				.isInstanceOf(OAuth2AuthenticationException.class);

		verifyNoInteractions(repository);
	}

	static OAuth2UserRequest createUserRequest(ClientRegistration clientRegistration) {
		return new OAuth2UserRequest(clientRegistration, OAuth2AccessTokens.createAccessToken("access-token"));
	}

	static OidcUserRequest createOidcRequest(ClientRegistration clientRegistration, AccountIdentity identity) {
		return new OidcUserRequest(clientRegistration, OAuth2AccessTokens.createAccessToken("access-token"),
				OAuth2AccessTokens.createIdToken("id-token", Map.of(
						IdTokenClaimNames.SUB, identity.getEmail(),
						StandardClaimNames.EMAIL, identity.getEmail(),
						StandardClaimNames.NAME, identity.getDisplayName()
				)));
	}

}
