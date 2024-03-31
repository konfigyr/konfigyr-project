package com.konfigyr.account;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Vladimir Spasic
 **/
class AccountTest {

	@Test
	@DisplayName("should create account using fluent builder")
	void shouldCreateAccount() {
		final var account = Account.builder()
				.id(12476518224L)
				.email("john.doe@konfigyr.com")
				.status(AccountStatus.ACTIVE)
				.firstName("John")
				.lastName("Doe")
				.avatar("https://example.com/avatar.gif")
				.lastLoginAt(Instant.now().minus(7, ChronoUnit.MINUTES))
				.createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
				.updatedAt(Instant.now().minus(3, ChronoUnit.HOURS))
				.build();

		assertThat(account)
				.returns(EntityId.from(12476518224L), Account::id)
				.returns("john.doe@konfigyr.com", Account::email)
				.returns(AccountStatus.ACTIVE, Account::status)
				.returns("John", Account::firstName)
				.returns("Doe", Account::lastName)
				.returns("John Doe", Account::displayName)
				.returns("https://example.com/avatar.gif", Account::avatar)
				.satisfies(it -> assertThat(it.lastLoginAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), byLessThan(8, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isEqualToIgnoringHours(OffsetDateTime.now().minusDays(10))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isEqualToIgnoringMinutes(OffsetDateTime.now(ZoneOffset.UTC).minusHours(3))
				);
	}

	@Test
	@DisplayName("should validate account data when using fluent builder")
	void shouldValidateAccountBuilder() {
		final var builder = Account.builder();

		assertThatThrownBy(builder::build)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Account entity identifier can not be null");

		assertThatThrownBy(() -> builder.id("000000BKTH3TG").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Account status can not be null");

		assertThatThrownBy(() -> builder.status(AccountStatus.INITIAL).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Account email address can not be blank");

		assertThat(builder.email("john.doe@konfigyr.com").build())
				.returns(EntityId.from(12476518224L), Account::id)
				.returns("john.doe@konfigyr.com", Account::email)
				.returns(AccountStatus.INITIAL, Account::status)
				.returns(null, Account::firstName)
				.returns(null, Account::lastName)
				.returns("john.doe@konfigyr.com", Account::displayName)
				.returns(null, Account::avatar)
				.returns(null, Account::lastLoginAt)
				.returns(null, Account::createdAt)
				.returns(null, Account::updatedAt);
	}

}