package com.konfigyr.security;

import com.konfigyr.account.AccountEvent;
import com.konfigyr.account.AccountManager;
import com.konfigyr.entity.EntityId;
import com.konfigyr.test.TestAccounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.cache.SpringCacheBasedUserCache;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountPrincipalServiceTest {

	@Mock
	AccountManager manager;

	PrincipalService service;

	@BeforeEach
	void setup() {
		service = new AccountPrincipalService(manager);
	}

	@Test
	@DisplayName("should load account by identifier")
	void loadByIdentifier() {
		final var account = TestAccounts.jane().build();

		doReturn(Optional.of(account)).when(manager).findById(account.id());

		assertThat(service.lookup(account.id().serialize()))
				.isNotNull()
				.isInstanceOf(AccountPrincipal.class)
				.returns(account.id().serialize(), UserDetails::getUsername)
				.returns("", UserDetails::getPassword)
				.returns(AuthorityUtils.createAuthorityList("admin"), UserDetails::getAuthorities)
				.returns(false, UserDetails::isEnabled)
				.returns(true, UserDetails::isAccountNonLocked)
				.returns(true, UserDetails::isAccountNonExpired)
				.returns(false, UserDetails::isCredentialsNonExpired);
	}

	@Test
	@DisplayName("should load account by serialized identifier")
	void loadBySerializedIdentifier() {
		final var account = TestAccounts.john().build();

		doReturn(Optional.of(account)).when(manager).findById(account.id());

		assertThat(service.lookup(account.id().serialize()))
				.isNotNull()
				.isInstanceOf(AccountPrincipal.class)
				.returns(account.id().serialize(), UserDetails::getUsername)
				.returns("", UserDetails::getPassword)
				.returns(AuthorityUtils.createAuthorityList("admin"), UserDetails::getAuthorities)
				.returns(true, UserDetails::isEnabled)
				.returns(true, UserDetails::isAccountNonLocked)
				.returns(true, UserDetails::isAccountNonExpired)
				.returns(true, UserDetails::isCredentialsNonExpired);
	}

	@Test
	@DisplayName("should load user from cache")
	void loadUserFromCache() {
		final var cache = new ConcurrentMapCache("user-cache");
		service = new AccountPrincipalService(manager, new SpringCacheBasedUserCache(cache));

		final var account = TestAccounts.john().build();

		doReturn(Optional.of(account)).when(manager).findById(account.id());

		final var principal = service.lookup(account.id().serialize());

		assertThat(principal)
				.isNotNull()
				.isSameAs(service.lookup(account.id().serialize()));

		assertThat(cache.getNativeCache())
				.isNotEmpty()
				.containsEntry(principal.getUsername(), principal);

		verify(manager, times(1)).findById(account.id());
	}

	@Test
	@DisplayName("should evict user from cache when invalid")
	void loadEvictInvalidPrincipalFromCache() {
		final var account = TestAccounts.john().build();
		final var invalid = User.withUsername("test")
				.password("pass")
				.authorities("testing")
				.build();

		final var cache = new ConcurrentMapCache("user-cache");
		cache.put(account.id().serialize(), invalid);

		service = new AccountPrincipalService(manager, new SpringCacheBasedUserCache(cache));

		assertThatThrownBy(() -> service.lookup(account.id()))
				.isInstanceOf(UsernameNotFoundException.class);

		assertThat(cache.getNativeCache())
				.isEmpty();

		verify(manager, times(1)).findById(account.id());
	}

	@Test
	@DisplayName("should fail to load account by identifier")
	void failToLoadByIdentifier() {
		final var id = EntityId.from(1);

		assertThatThrownBy(() -> service.lookup(id))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("Failed to lookup user account for identifier:")
				.hasMessageContaining(id.toString())
				.hasNoCause();

		verify(manager).findById(id);
	}

	@Test
	@DisplayName("should fail to load account by serialized identifier")
	void failToLoadBySerializedIdentifier() {
		final var id = EntityId.from(1);

		assertThatThrownBy(() -> service.lookup(id.serialize()))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("Failed to lookup user account for identifier:")
				.hasMessageContaining(id.toString())
				.hasNoCause();

		verify(manager).findById(id);
	}

	@Test
	@DisplayName("should fail load account when identifier is empty, blank or invalid")
	void failToLoadWhenBlankOrInvalid() {
		assertThatThrownBy(() -> service.lookup(""))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("Failed to lookup user account for invalid username")
				.hasRootCauseInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> service.lookup("   "))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("Failed to lookup user account for invalid username")
				.hasRootCauseInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> service.lookup("account@acme.com"))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("Failed to lookup user account for invalid username")
				.hasRootCauseInstanceOf(IllegalArgumentException.class);

		verifyNoInteractions(manager);
	}

	@Test
	@DisplayName("should evict account from cache when account is updated or deleted")
	void shouldClearCacheOnAccountUpdated() {
		final var cache = mock(UserCache.class);
		final var service = new AccountPrincipalService(manager, cache);

		assertThatNoException().isThrownBy(
				() -> service.onAccountUpdatedEvent(new AccountEvent.Updated(EntityId.from(46)))
		);

		assertThatNoException().isThrownBy(
				() -> service.onAccountDeletedEvent(new AccountEvent.Deleted(EntityId.from(51)))
		);

		verify(cache).removeUserFromCache(EntityId.from(46).serialize());
		verify(cache).removeUserFromCache(EntityId.from(51).serialize());
	}

}