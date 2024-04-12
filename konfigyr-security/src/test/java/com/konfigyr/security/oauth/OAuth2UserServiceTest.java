package com.konfigyr.security.oauth;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountManager;
import com.konfigyr.account.AccountRegistration;
import com.konfigyr.security.AccountPrincipal;
import com.konfigyr.security.AccountPrincipalService;
import com.konfigyr.test.TestAccounts;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OAuth2UserServiceTest {

	@Mock
	AccountManager manager;

	@Mock
	ClientRegistration registration;

	@Mock
	OAuth2UserService<OAuth2UserRequest, ? extends OAuth2User> delegate;

	Account account;
	OAuth2UserService<OAuth2UserRequest, ? extends OAuth2User> service;

	@BeforeEach
	void setup() {
		account = TestAccounts.john().avatar("https://example.com/avatar.svg").build();
		service = new PrincipalAccountOAuth2UserService(new AccountPrincipalService(manager), delegate);
	}

	@Test
	@DisplayName("should retrieve account for registration from delegate")
	void shouldRegisterAccount() {
		final var request = new OAuth2UserRequest(registration, OAuth2AccessTokens.createAccessToken("token"));

		doReturn("github").when(registration).getRegistrationId();
		doReturn(createUser()).when(delegate).loadUser(request);
		doReturn(account).when(manager).create(any());

		assertThat(service.loadUser(request))
				.isInstanceOf(AccountPrincipal.class)
				.returns(account.id().serialize(), OAuth2User::getName)
				.returns(Map.of(), OAuth2User::getAttributes)
				.asInstanceOf(InstanceOfAssertFactories.type(AccountPrincipal.class))
				.returns(account, AccountPrincipal::getAccount)
				.returns(account.email(), AccountPrincipal::getEmail)
				.returns(account.avatar(), AccountPrincipal::getAvatar)
				.returns(account.displayName(), AccountPrincipal::getDisplayName);

		final var captor = ArgumentCaptor.forClass(AccountRegistration.class);
		verify(manager).create(captor.capture());

		assertThat(captor.getValue())
				.isNotNull()
				.returns(account.email(), AccountRegistration::email)
				.returns(account.firstName(), AccountRegistration::firstName)
				.returns(account.lastName(), AccountRegistration::lastName)
				.returns(account.avatar(), AccountRegistration::avatar);
	}

	@Test
	@DisplayName("should retrieve existing account from delegate")
	void shouldLoadExistingAccount() {
		final var request = new OAuth2UserRequest(registration, OAuth2AccessTokens.createAccessToken("token"));

		doReturn(createUser()).when(delegate).loadUser(request);
		doReturn(Optional.of(account)).when(manager).findByEmail("john.doe@konfigyr.com");

		assertThat(service.loadUser(request))
				.isInstanceOf(AccountPrincipal.class)
				.returns(account.id().serialize(), OAuth2User::getName)
				.returns(Map.of(), OAuth2User::getAttributes)
				.asInstanceOf(InstanceOfAssertFactories.type(AccountPrincipal.class))
				.returns(account, AccountPrincipal::getAccount)
				.returns(account.email(), AccountPrincipal::getEmail)
				.returns(account.avatar(), AccountPrincipal::getAvatar)
				.returns(account.displayName(), AccountPrincipal::getDisplayName);

		verify(manager, times(0)).create(any());
	}

	@Test
	@DisplayName("should fail to find user")
	void shouldFailToFindUser() {
		final var request = new OAuth2UserRequest(registration, OAuth2AccessTokens.createAccessToken("token"));

		doReturn(null).when(delegate).loadUser(request);

		assertThat(service.loadUser(request))
				.isNull();

		verifyNoInteractions(manager);
	}

	@Test
	@DisplayName("should fail to load user")
	void shouldFailToLoadUser() {
		final var request = new OAuth2UserRequest(registration, OAuth2AccessTokens.createAccessToken("token"));

		doThrow(OAuth2AuthenticationException.class).when(delegate).loadUser(request);

		assertThatThrownBy(() -> service.loadUser(request))
				.isInstanceOf(OAuth2AuthenticationException.class);

		verifyNoInteractions(manager);
	}

	@Test
	@DisplayName("should fail to find user attribute converter")
	void shouldFailToFindConverter() {
		final var request = new OAuth2UserRequest(registration, OAuth2AccessTokens.createAccessToken("token"));

		doReturn("unknown").when(registration).getRegistrationId();
		doReturn(createUser()).when(delegate).loadUser(request);

		assertThatThrownBy(() -> service.loadUser(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unsupported OAuth Client registration")
				.hasMessageContaining(registration.getRegistrationId())
				.hasNoCause();
	}

	static OAuth2User createUser() {
		return new DefaultOAuth2User(
				AuthorityUtils.createAuthorityList("test-scope"),
				Map.of(
						"email", "john.doe@konfigyr.com",
						"name", "John Doe",
						"avatar_url", "https://example.com/avatar.svg"
				),
				"email"
		);
	}
}