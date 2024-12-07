package com.konfigyr.security;

import com.konfigyr.account.AccountEvent;
import com.konfigyr.account.AccountManager;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Member;
import com.konfigyr.namespace.NamespaceEvent;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.test.TestAccounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.cache.SpringCacheBasedUserCache;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountPrincipalServiceTest {

	@Mock
	AccountManager accounts;

	@Mock
	NamespaceManager namespaces;

	PrincipalService service;

	@BeforeEach
	void setup() {
		service = new AccountPrincipalService(accounts, namespaces);
	}

	@Test
	@DisplayName("should load account by identifier")
	void loadByIdentifier() {
		final var account = TestAccounts.jane().build();

		doReturn(Optional.of(account)).when(accounts).findById(account.id());

		assertThat(service.lookup(account.id().serialize()))
				.isNotNull()
				.isInstanceOf(AccountPrincipal.class)
				.returns(account.id().serialize(), UserDetails::getUsername)
				.returns("", UserDetails::getPassword)
				.returns(true, UserDetails::isEnabled)
				.returns(true, UserDetails::isAccountNonLocked)
				.returns(true, UserDetails::isAccountNonExpired)
				.returns(true, UserDetails::isCredentialsNonExpired)
				.satisfies(it -> assertThat(it.getAuthorities())
						.extracting(GrantedAuthority::getAuthority)
						.containsExactlyInAnyOrder("konfigyr:user")
				);
	}

	@Test
	@DisplayName("should load account by serialized identifier")
	void loadBySerializedIdentifier() {
		final var account = TestAccounts.john().build();

		doReturn(Optional.of(account)).when(accounts).findById(account.id());

		assertThat(service.lookup(account.id().serialize()))
				.isNotNull()
				.isInstanceOf(AccountPrincipal.class)
				.returns(account.id().serialize(), UserDetails::getUsername)
				.returns("", UserDetails::getPassword)
				.returns(true, UserDetails::isEnabled)
				.returns(true, UserDetails::isAccountNonLocked)
				.returns(true, UserDetails::isAccountNonExpired)
				.returns(true, UserDetails::isCredentialsNonExpired)
				.satisfies(it -> assertThat(it.getAuthorities())
						.extracting(GrantedAuthority::getAuthority)
						.containsExactlyInAnyOrder("konfigyr:admin", "john-doe:admin")
				);
	}

	@Test
	@DisplayName("should load user from cache")
	void loadUserFromCache() {
		final var cache = new ConcurrentMapCache("user-cache");
		service = new AccountPrincipalService(accounts, namespaces, new SpringCacheBasedUserCache(cache));

		final var account = TestAccounts.john().build();

		doReturn(Optional.of(account)).when(accounts).findById(account.id());

		final var principal = service.lookup(account.id().serialize());

		assertThat(principal)
				.isNotNull()
				.isSameAs(service.lookup(account.id().serialize()));

		assertThat(cache.getNativeCache())
				.isNotEmpty()
				.containsEntry(principal.getUsername(), principal);

		verify(accounts, times(1)).findById(account.id());
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

		service = new AccountPrincipalService(accounts, namespaces, new SpringCacheBasedUserCache(cache));

		assertThatThrownBy(() -> service.lookup(account.id()))
				.isInstanceOf(UsernameNotFoundException.class);

		assertThat(cache.getNativeCache())
				.isEmpty();

		verify(accounts, times(1)).findById(account.id());
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

		verify(accounts).findById(id);
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

		verify(accounts).findById(id);
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

		verifyNoInteractions(accounts);
	}

	@Test
	@DisplayName("should evict account from cache when account is updated or deleted")
	void shouldClearCacheOnAccountEvents() {
		final var cache = mock(UserCache.class);
		final var authentication = mock(Authentication.class);
		final var service = new AccountPrincipalService(accounts, namespaces, cache);

		final var context = SecurityContextHolder.getContextHolderStrategy().getContext();
		context.setAuthentication(authentication);

		assertThatNoException().isThrownBy(
				() -> service.onAccountUpdatedEvent(new AccountEvent.Updated(EntityId.from(46)))
		);

		assertThat(context.getAuthentication())
				.isNull();

		assertThatNoException().isThrownBy(
				() -> service.onAccountDeletedEvent(new AccountEvent.Deleted(EntityId.from(51)))
		);

		assertThat(context.getAuthentication())
				.isNull();

		verify(cache).removeUserFromCache(EntityId.from(46).serialize());
		verify(cache).removeUserFromCache(EntityId.from(51).serialize());
	}

	@Test
	@DisplayName("should evict all namespace member accounts from cache when namespace is deleted")
	void shouldClearCacheOnNamespaceDeletedEvent() {
		final var cache = mock(UserCache.class);
		final var member = mock(Member.class);
		final var service = new AccountPrincipalService(accounts, namespaces, cache);
		final var members = List.of(member, member, member, member);

		doReturn(new PageImpl<>(members)).when(namespaces).findMembers(EntityId.from(65), Pageable.unpaged());
		doReturn(EntityId.from(1), EntityId.from(2), EntityId.from(3), EntityId.from(4))
				.when(member).account();

		assertThatNoException().isThrownBy(
				() -> service.onNamespaceDeletedEvent(new NamespaceEvent.Deleted(EntityId.from(65)))
		);

		verify(cache).removeUserFromCache(EntityId.from(1).serialize());
		verify(cache).removeUserFromCache(EntityId.from(2).serialize());
		verify(cache).removeUserFromCache(EntityId.from(3).serialize());
		verify(cache).removeUserFromCache(EntityId.from(4).serialize());
	}

	@Test
	@DisplayName("should catch namespace manager exceptions when when namespace is deleted")
	void shouldCatchNamespaceManagerExceptions() {
		final var cache = mock(UserCache.class);
		final var service = new AccountPrincipalService(accounts, namespaces, cache);

		doThrow(RuntimeException.class).when(namespaces).findMembers(EntityId.from(80), Pageable.unpaged());

		assertThatNoException().isThrownBy(
				() -> service.onNamespaceDeletedEvent(new NamespaceEvent.Deleted(EntityId.from(80)))
		);

		verifyNoInteractions(cache);
	}

	@Test
	@DisplayName("should evict account from cache when member is added to namespace")
	void shouldClearCacheOnNamespaceMembershipAddedEvent() {
		final var cache = mock(UserCache.class);
		final var service = new AccountPrincipalService(accounts, namespaces, cache);

		assertThatNoException().isThrownBy(() -> service.onMemberAddedEvent(
				new NamespaceEvent.MemberAdded(EntityId.from(1), EntityId.from(23), NamespaceRole.ADMIN)
		));

		verify(cache).removeUserFromCache(EntityId.from(23).serialize());
	}

	@Test
	@DisplayName("should evict account from cache when namespace member is updated")
	void shouldClearCacheOnNamespaceMembershipUpdatedEvent() {
		final var cache = mock(UserCache.class);
		final var service = new AccountPrincipalService(accounts, namespaces, cache);

		assertThatNoException().isThrownBy(() -> service.onMemberUpdatedEvent(
				new NamespaceEvent.MemberUpdated(EntityId.from(3), EntityId.from(87), NamespaceRole.ADMIN)
		));

		verify(cache).removeUserFromCache(EntityId.from(87).serialize());
	}

	@Test
	@DisplayName("should evict account from cache when namespace member is removed")
	void shouldClearCacheOnNamespaceMembershipRemovedEvent() {
		final var cache = mock(UserCache.class);
		final var service = new AccountPrincipalService(accounts, namespaces, cache);

		assertThatNoException().isThrownBy(() -> service.onMemberRemovedEvent(
				new NamespaceEvent.MemberRemoved(EntityId.from(98), EntityId.from(12))
		));

		verify(cache).removeUserFromCache(EntityId.from(12).serialize());
	}

}
