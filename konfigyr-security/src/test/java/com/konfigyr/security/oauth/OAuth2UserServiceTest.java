package com.konfigyr.security.oauth;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountManager;
import com.konfigyr.namespace.NamespaceType;
import com.konfigyr.security.AccountPrincipal;
import com.konfigyr.security.AccountPrincipalService;
import com.konfigyr.security.provisioning.ProvisioningHints;
import com.konfigyr.security.provisioning.ProvisioningRequiredException;
import com.konfigyr.support.FullName;
import com.konfigyr.test.TestAccounts;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.net.URI;
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
				.returns(account.email(), AccountPrincipal::getEmail)
				.returns(account.avatar(), AccountPrincipal::getAvatar)
				.returns(account.displayName(), AccountPrincipal::getDisplayName);

		verify(manager).findByEmail("john.doe@konfigyr.com");
		verify(manager, times(0)).create(any());
	}



	@Test
	@DisplayName("should throw provisioning required exception when account does not exist")
	void shouldThrowProvisioningRequiredException() {
		final var request = new OAuth2UserRequest(registration, OAuth2AccessTokens.createAccessToken("token"));

		doReturn("github").when(registration).getRegistrationId();
		doReturn(createUser()).when(delegate).loadUser(request);

		assertThatThrownBy(() -> service.loadUser(request))
				.isInstanceOf(ProvisioningRequiredException.class)
				.asInstanceOf(InstanceOfAssertFactories.type(ProvisioningRequiredException.class))
				.extracting(ProvisioningRequiredException::getHints)
				.returns("john.doe@konfigyr.com", ProvisioningHints::getEmail)
				.returns(FullName.parse("John Doe"), ProvisioningHints::getName)
				.returns(URI.create("https://example.com/avatar.svg"), ProvisioningHints::getAvatar)
				.returns(null, ProvisioningHints::getNamespace)
				.returns(NamespaceType.PERSONAL, ProvisioningHints::getType);

		verify(manager).findByEmail("john.doe@konfigyr.com");
		verify(manager, times(0)).create(any());
	}

	@Test
	@DisplayName("should fail to find OAUth user atributes from delegating user service")
	void shouldFailToFindUser() {
		final var request = new OAuth2UserRequest(registration, OAuth2AccessTokens.createAccessToken("token"));

		doReturn(null).when(delegate).loadUser(request);

		assertThat(service.loadUser(request))
				.isNull();

		verifyNoInteractions(manager);
	}

	@Test
	@DisplayName("should rethrow exceptions thrown by the delegating user service")
	void shouldFailToLoadUser() {
		final var request = new OAuth2UserRequest(registration, OAuth2AccessTokens.createAccessToken("token"));

		doThrow(OAuth2AuthenticationException.class).when(delegate).loadUser(request);

		assertThatThrownBy(() -> service.loadUser(request))
				.isInstanceOf(OAuth2AuthenticationException.class);

		verifyNoInteractions(manager);
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