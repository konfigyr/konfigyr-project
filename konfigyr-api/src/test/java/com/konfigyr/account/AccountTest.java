package com.konfigyr.account;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

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

	@Test
	@DisplayName("should create account registration using fluent builder")
	void shouldCreateAccountRegistration() {
		final var registration = AccountRegistration.builder()
				.email("john.doe@konfigyr.com")
				.firstName("John")
				.lastName("Doe")
				.avatar("https://example.com/avatar.gif")
				.build();

		assertThat(registration)
				.returns("john.doe@konfigyr.com", AccountRegistration::email)
				.returns("John", AccountRegistration::firstName)
				.returns("Doe", AccountRegistration::lastName)
				.returns("https://example.com/avatar.gif", AccountRegistration::avatar);
	}

	@Test
	@DisplayName("should fail to create account registration without email address")
	void shouldValidateAccountRegistrationBuilder() {
		final var builder = AccountRegistration.builder();

		assertThatThrownBy(builder::build)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Account email address can not be blank");

		assertThat(builder.email("john.doe@konfigyr.com").build())
				.returns("john.doe@konfigyr.com", AccountRegistration::email)
				.returns(null, AccountRegistration::firstName)
				.returns(null, AccountRegistration::lastName)
				.returns(null, AccountRegistration::avatar);
	}

	@Test
	@DisplayName("should create an account registered event")
	void shouldCreateRegistrationEvent() {
		assertThat(AccountEvent.registered(EntityId.from(8366545L)))
				.isInstanceOf(AccountEvent.Registered.class)
				.returns(EntityId.from(8366545L), EntityEvent::id);
	}

	@Test
	@DisplayName("should create account exists exception")
	void shouldCreateAccountExistsException() {
		final var registration = AccountRegistration.builder()
				.email("john.doe@konfigyr.com")
				.build();

		assertThat(new AccountExistsException(registration))
				.hasMessage("Could not register account as one already exists with that email address")
				.hasNoCause()
				.returns(registration, AccountExistsException::getRegistration);
	}

	@Test
	@DisplayName("should create account exists exception with cause")
	void shouldCreateAccountExistsExceptionWithCause() {
		final var cause = new Exception("Cause");
		final var registration = AccountRegistration.builder()
				.email("john.doe@konfigyr.com")
				.build();

		assertThat(new AccountExistsException(registration, cause))
				.hasMessage("Could not register account as one already exists with that email address")
				.hasCause(cause)
				.returns(registration, AccountExistsException::getRegistration);
	}

	@MethodSource("names")
	@ParameterizedTest(name = "should parse full name: \"{0}\" into \"{1}\" \"{2}\"")
	@DisplayName("should create account registration with full name")
	void shouldCreateAccountRegistrationUsingFullName(String fullName, String firstName, String lastname) {
		final var registration = AccountRegistration.builder()
				.email("john.doe@konfigyr.com")
				.fullName(fullName)
				.build();

		assertThat(registration)
				.returns("john.doe@konfigyr.com", AccountRegistration::email)
				.returns(firstName, AccountRegistration::firstName)
				.returns(lastname, AccountRegistration::lastName)
				.returns(null, AccountRegistration::avatar);
	}

	static Stream<Arguments> names() {
		return Stream.of(
				Arguments.of(null, null, null),
				Arguments.of("", null, null),
				Arguments.of("   ", null, null),
				Arguments.of("Stilgar", "Stilgar", null),
				Arguments.of("Paul Atreides", "Paul",  "Atreides"),
				Arguments.of("Liet-Kynes", "Liet-Kynes", null),
				Arguments.of("Piter De Vries", "Piter", "De Vries")
		);
	}

}