package com.konfigyr.security;

import com.konfigyr.test.TestAccounts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccountPrincipalTest {

	@Test
	@DisplayName("should create account user")
	void shouldCreateUserFromAccount() {
		final var account = TestAccounts.john().build();

		assertThat(new AccountPrincipal(account))
				.returns(account.id(), AccountPrincipal::getId)
				.returns(account.email(), AccountPrincipal::getEmail)
				.returns(account.id().serialize(), UserDetails::getUsername)
				.returns(account.avatar(), AccountPrincipal::getAvatar)
				.returns(Map.of(), AccountPrincipal::getAttributes)
				.returns("", UserDetails::getPassword)
				.returns(AuthorityUtils.createAuthorityList("admin"), UserDetails::getAuthorities)
				.returns(true, UserDetails::isEnabled)
				.returns(true, UserDetails::isAccountNonLocked)
				.returns(true, UserDetails::isAccountNonExpired)
				.returns(true, UserDetails::isCredentialsNonExpired)
				.hasSameHashCodeAs(new AccountPrincipal(account))
				.isEqualTo(new AccountPrincipal(account));
	}

}