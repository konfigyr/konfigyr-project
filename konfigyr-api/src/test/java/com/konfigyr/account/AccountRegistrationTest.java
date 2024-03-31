package com.konfigyr.account;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.byLessThan;

/**
 * @author Vladimir Spasic
 **/
class AccountRegistrationTest {

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
	@DisplayName("should fail to create account without email address")
	void shouldValidateAccountBuilder() {
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
		assertThat(new AccountRegisteredEvent(EntityId.from(8366545L)))
				.returns(EntityId.from(8366545L), EntityEvent::id)
				.satisfies(it -> assertThat(it.timestamp())
						.isCloseTo(Instant.now(), byLessThan(600, ChronoUnit.MILLIS))
				).isNotEqualTo(
						new AccountRegisteredEvent(EntityId.from(8366545L))
				);
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