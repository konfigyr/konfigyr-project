package com.konfigyr.security;

import com.konfigyr.account.AccountStatus;
import com.konfigyr.test.TestAccounts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccountPrincipalTest {

	@Test
	@DisplayName("should create active account principal")
	void shouldCreatePrincipalFromActiveAccount() {
		final var account = TestAccounts.john().build();

		assertThat(AccountPrincipal.from(account))
				.returns(account.id(), AccountPrincipal::getId)
				.returns(account.email(), AccountPrincipal::getEmail)
				.returns(account.id().serialize(), UserDetails::getUsername)
				.returns(account.displayName(), AccountPrincipal::getDisplayName)
				.returns(account.avatar(), AccountPrincipal::getAvatar)
				.returns(account.memberships(), AccountPrincipal::getMemberships)
				.returns(Map.of(), AccountPrincipal::getAttributes)
				.returns("", UserDetails::getPassword)
				.returns(true, UserDetails::isEnabled)
				.returns(true, UserDetails::isAccountNonLocked)
				.returns(true, UserDetails::isAccountNonExpired)
				.returns(true, UserDetails::isCredentialsNonExpired)
				.satisfies(it -> assertThat(it.getAuthorities())
						.extracting(GrantedAuthority::getAuthority)
						.containsExactlyInAnyOrder("konfigyr:admin", "john-doe:admin")
				)
				.hasSameHashCodeAs(AccountPrincipal.from(account))
				.isEqualTo(AccountPrincipal.from(account));
	}

	@Test
	@DisplayName("should create suspended account principal")
	void shouldCreatePrincipalFromInactiveAccount() {
		final var account = TestAccounts.jane()
				.status(AccountStatus.SUSPENDED)
				.build();

		assertThat(AccountPrincipal.from(account))
				.returns(account.id(), AccountPrincipal::getId)
				.returns(account.email(), AccountPrincipal::getEmail)
				.returns(account.id().serialize(), UserDetails::getUsername)
				.returns(account.avatar(), AccountPrincipal::getAvatar)
				.returns(account.memberships(), AccountPrincipal::getMemberships)
				.returns(Map.of(), AccountPrincipal::getAttributes)
				.returns("", UserDetails::getPassword)
				.returns(false, UserDetails::isEnabled)
				.returns(false, UserDetails::isAccountNonLocked)
				.returns(true, UserDetails::isAccountNonExpired)
				.returns(false, UserDetails::isCredentialsNonExpired)
				.satisfies(it -> assertThat(it.getAuthorities())
						.extracting(GrantedAuthority::getAuthority)
						.containsExactlyInAnyOrder("konfigyr:user")
				)
				.hasSameHashCodeAs(AccountPrincipal.from(account))
				.isEqualTo(AccountPrincipal.from(account));
	}

	@Test
	@DisplayName("should create deactivated account principal")
	void shouldCreatePrincipalFromDeactivatedAccount() {
		final var account = TestAccounts.john()
				.status(AccountStatus.DEACTIVATED)
				.build();

		assertThat(AccountPrincipal.from(account))
				.returns(account.id(), AccountPrincipal::getId)
				.returns(account.email(), AccountPrincipal::getEmail)
				.returns(account.id().serialize(), UserDetails::getUsername)
				.returns(account.avatar(), AccountPrincipal::getAvatar)
				.returns(Map.of(), AccountPrincipal::getAttributes)
				.returns(account.memberships(), AccountPrincipal::getMemberships)
				.returns("", UserDetails::getPassword)
				.returns(false, UserDetails::isEnabled)
				.returns(true, UserDetails::isAccountNonLocked)
				.returns(false, UserDetails::isAccountNonExpired)
				.returns(false, UserDetails::isCredentialsNonExpired)
				.satisfies(it -> assertThat(it.getAuthorities())
						.extracting(GrantedAuthority::getAuthority)
						.containsExactlyInAnyOrder("konfigyr:admin", "john-doe:admin")
				)
				.hasSameHashCodeAs(AccountPrincipal.from(account))
				.isEqualTo(AccountPrincipal.from(account));
	}

	@Test
	@DisplayName("should create suspended account principal")
	void shouldCreatePrincipalFromSuspendedAccount() {
		final var account = TestAccounts.john()
				.status(AccountStatus.SUSPENDED)
				.build();

		assertThat(AccountPrincipal.from(account))
				.returns(account.id(), AccountPrincipal::getId)
				.returns(account.email(), AccountPrincipal::getEmail)
				.returns(account.id().serialize(), UserDetails::getUsername)
				.returns(account.avatar(), AccountPrincipal::getAvatar)
				.returns(account.memberships(), AccountPrincipal::getMemberships)
				.returns(Map.of(), AccountPrincipal::getAttributes)
				.returns("", UserDetails::getPassword)
				.returns(false, UserDetails::isEnabled)
				.returns(false, UserDetails::isAccountNonLocked)
				.returns(true, UserDetails::isAccountNonExpired)
				.returns(false, UserDetails::isCredentialsNonExpired)
				.satisfies(it -> assertThat(it.getAuthorities())
						.extracting(GrantedAuthority::getAuthority)
						.containsExactlyInAnyOrder("konfigyr:admin", "john-doe:admin")
				)
				.hasSameHashCodeAs(AccountPrincipal.from(account))
				.isEqualTo(AccountPrincipal.from(account));
	}

}