package com.konfigyr.account;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.namespace.NamespaceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;


class AccountTest {

	@Test
	@DisplayName("should create account using fluent builder")
	void shouldCreateAccount() {
		final var membership = Membership.builder()
				.id(1L)
				.type(NamespaceType.PERSONAL)
				.role(NamespaceRole.ADMIN)
				.namespace("john.doe")
				.name("John Doe")
				.build();

		final var account = Account.builder()
				.id(12476518224L)
				.email("john.doe@konfigyr.com")
				.status(AccountStatus.ACTIVE)
				.firstName("John")
				.lastName("Doe")
				.avatar("https://example.com/avatar.gif")
				.membership(null)
				.memberships(List.of(membership))
				.membership(null)
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
				.returns(true, Account::isDeletable)
				.returns(Memberships.of(membership), Account::memberships)
				.satisfies(it -> assertThat(it.lastLoginAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(ZoneOffset.UTC), byLessThan(8, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isEqualToIgnoringHours(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isEqualToIgnoringMinutes(OffsetDateTime.now(ZoneOffset.UTC).minusHours(3))
				);

		assertThat(Account.builder(account).build())
				.isEqualTo(account);
	}

	@Test
	@DisplayName("should check if account can be deleted")
	void verifyAccountDeleteCheck() {
		final var builder = Account.builder()
				.id(31L)
				.status(AccountStatus.ACTIVE)
				.email("admin@konfigyr.com");

		// no memberships, can delete
		assertThat(builder.build().isDeletable()).isTrue();

		final var personal = Membership.builder()
				.id(1L)
				.type(NamespaceType.PERSONAL)
				.role(NamespaceRole.ADMIN)
				.namespace("personal")
				.name("Personal")
				.build();

		// personal membership only, can delete
		assertThat(builder.membership(personal).build().isDeletable()).isTrue();

		final var team = Membership.builder()
				.id(2L)
				.type(NamespaceType.TEAM)
				.role(NamespaceRole.USER)
				.namespace("team")
				.name("Team")
				.build();

		// personal membership + user in team namespace, can delete
		assertThat(builder.membership(team).build().isDeletable()).isTrue();

		final var org = Membership.builder()
				.id(2L)
				.type(NamespaceType.ENTERPRISE)
				.role(NamespaceRole.ADMIN)
				.namespace("org")
				.name("Org")
				.build();

		// personal membership + user in team namespace + admin in org namespace, can not delete
		assertThat(builder.membership(org).build().isDeletable()).isFalse();
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

		assertThatThrownBy(() -> builder.status("INITIAL").build())
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
				.returns(true, Account::isDeletable)
				.returns(Memberships.empty(), Account::memberships)
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
	@DisplayName("should create account registration using fluent builder with full names")
	void shouldCreateAccountRegistrationWithFullName() {
		final var builder = AccountRegistration.builder()
				.email("john.doe@konfigyr.com")
				.fullName(null);

		assertThat(builder.build())
				.returns("john.doe@konfigyr.com", AccountRegistration::email)
				.returns(null, AccountRegistration::firstName)
				.returns(null, AccountRegistration::lastName);

		assertThat(builder.fullName("John").build())
				.returns("john.doe@konfigyr.com", AccountRegistration::email)
				.returns("John", AccountRegistration::firstName)
				.returns("", AccountRegistration::lastName);

		assertThat(builder.fullName("Jane Doe").build())
				.returns("john.doe@konfigyr.com", AccountRegistration::email)
				.returns("Jane", AccountRegistration::firstName)
				.returns("Doe", AccountRegistration::lastName);
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
	@DisplayName("should create membership using fluent builder")
	void shouldCreateMembership() {
		final var membership = Membership.builder()
				.id(65L)
				.type(NamespaceType.TEAM)
				.role(NamespaceRole.USER)
				.namespace("konfigyr")
				.name("Konfigyr")
				.avatar("https://example.com/avatar.gif")
				.since(Instant.now())
				.build();

		assertThat(membership)
				.returns(EntityId.from(65), Membership::id)
				.returns(NamespaceRole.USER, Membership::role)
				.returns(NamespaceType.TEAM, Membership::type)
				.returns("konfigyr", Membership::namespace)
				.returns("Konfigyr", Membership::name)
				.returns("https://example.com/avatar.gif", Membership::avatar)
				.satisfies(it -> assertThat(it.since())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(ZoneOffset.UTC), byLessThan(300, ChronoUnit.MILLIS))
				);
	}

	@Test
	@DisplayName("should create memberships")
	void shouldCreateMemberships() {
		final var paul = Membership.builder()
				.id(19L)
				.type(NamespaceType.PERSONAL)
				.role(NamespaceRole.ADMIN)
				.namespace("paul.atreides")
				.name("Paul Atreides")
				.since(Instant.now())
				.build();

		final var atreides = Membership.builder()
				.id(22L)
				.type(NamespaceType.ENTERPRISE)
				.role(NamespaceRole.ADMIN)
				.namespace("atreides")
				.name("Atreides")
				.since(Instant.now().plusSeconds(60))
				.build();

		final var freemen = Membership.builder()
				.id(13L)
				.type(NamespaceType.TEAM)
				.role(NamespaceRole.USER)
				.namespace("freemen")
				.name("Freemen")
				.since(Instant.now().plusSeconds(180))
				.build();

		final var empire = Membership.builder()
				.id(2L)
				.type(NamespaceType.ENTERPRISE)
				.role(NamespaceRole.ADMIN)
				.namespace("empire")
				.name("Empire")
				.build();

		final var memberships = Memberships.of(freemen, empire, paul, atreides);

		assertThat(memberships)
				.hasSize(4)
				.containsExactly(paul, atreides, freemen, empire)
				.isEqualTo(Memberships.of(freemen, atreides, empire, paul))
				.isNotEqualTo(Memberships.of(freemen, empire, paul))
				.hasSameHashCodeAs(Memberships.of(paul, freemen, empire, atreides))
				.doesNotHaveSameHashCodeAs(Memberships.of(freemen, empire, paul));

		assertThatObject(memberships)
				.returns("paul.atreides, atreides, freemen, empire", Memberships::join)
				.extracting(it -> it.ofType(NamespaceType.ENTERPRISE))
				.returns("atreides, empire", Memberships::join);

		assertThat(memberships.ofType(NamespaceType.PERSONAL))
				.hasSize(1)
				.containsExactly(paul);

		assertThat(memberships.ofType(NamespaceType.TEAM))
				.hasSize(1)
				.containsExactly(freemen);

		assertThat(memberships.ofType(NamespaceType.ENTERPRISE))
				.hasSize(2)
				.containsExactly(atreides, empire);

		assertThat(memberships.ofType(NamespaceType.PERSONAL))
				.hasSize(1)
				.containsExactly(paul);

		assertThat(memberships.ofRole(NamespaceRole.ADMIN))
				.hasSize(3)
				.containsExactly(paul, atreides, empire);

		assertThat(memberships.ofRole(NamespaceRole.USER))
				.hasSize(1)
				.containsExactly(freemen);
	}

	@Test
	@DisplayName("should create empty memberships")
	void shouldCreateEmptyMemberships() {
		assertThat(Memberships.empty())
				.hasSize(0)
				.isSameAs(Memberships.of())
				.isSameAs(Memberships.of(List.of()))
				.isSameAs(Memberships.of((Membership[]) null));
	}

	@Test
	@DisplayName("should validate membership builder")
	void shouldValidateMembershipBuilder() {
		final var builder = Membership.builder();

		assertThatThrownBy(builder::build)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Member entity identifier can not be null");

		assertThatThrownBy(() -> builder.id(EntityId.from(5).serialize()).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace type can not be null");

		assertThatThrownBy(() -> builder.type("ENTERPRISE").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace role can not be null");

		assertThatThrownBy(() -> builder.role("USER").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace slug can not be blank");

		assertThatThrownBy(() -> builder.namespace("konfigyr").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace name can not be blank");

		assertThat(builder.name("Konfigyr").build())
				.returns(EntityId.from(5), Membership::id)
				.returns(NamespaceType.ENTERPRISE, Membership::type)
				.returns(NamespaceRole.USER, Membership::role)
				.returns("konfigyr", Membership::namespace)
				.returns("Konfigyr", Membership::name)
				.returns(null, Membership::avatar)
				.returns(null, Membership::since);
	}

	@Test
	@DisplayName("should create an account events")
	void shouldCreateAccountEvents() {
		assertThat(new AccountEvent.Registered(EntityId.from(8366545L)))
				.isInstanceOf(AccountEvent.class)
				.returns(EntityId.from(8366545L), EntityEvent::id);

		assertThat(new AccountEvent.Updated(EntityId.from(8366545L)))
				.isInstanceOf(AccountEvent.class)
				.returns(EntityId.from(8366545L), EntityEvent::id);

		assertThat(new AccountEvent.Deleted(EntityId.from(8366545L)))
				.isInstanceOf(AccountEvent.class)
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

	@Test
	@DisplayName("should create account not found exception")
	void shouldCreateAccountNotFoundException() {
		assertThat(new AccountNotFoundException(EntityId.from(6)))
				.hasMessageContaining("Failed to find account with entity identifier")
				.hasMessageContaining(EntityId.from(6).toString())
				.hasNoCause();
	}

}