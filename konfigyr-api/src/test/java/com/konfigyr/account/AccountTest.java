package com.konfigyr.account;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.support.Avatar;
import com.konfigyr.support.FullName;
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
				.returns(FullName.of("John", "Doe"), Account::fullName)
				.returns(Avatar.parse("https://example.com/avatar.gif"), Account::avatar)
				.returns(false, Account::isDeletable)
				.returns(Memberships.of(membership), Account::memberships)
				.satisfies(it -> assertThat(it.lastLoginAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(ZoneOffset.UTC), byLessThan(8, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10), within(1, ChronoUnit.SECONDS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(ZoneOffset.UTC).minusHours(3), within(1, ChronoUnit.SECONDS))
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

		final var team = Membership.builder()
				.id(2L)
				.role(NamespaceRole.USER)
				.namespace("team")
				.name("Team")
				.build();

		// user in team namespace, can delete
		assertThat(builder.membership(team).build().isDeletable()).isTrue();

		final var org = Membership.builder()
				.id(2L)
				.role(NamespaceRole.ADMIN)
				.namespace("org")
				.name("Org")
				.build();

		// user in team namespace + admin in org namespace, can not delete
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

		assertThatThrownBy(() -> builder.status("SUSPENDED").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Account email address can not be blank");

		assertThat(builder.email("john.doe@konfigyr.com").build())
				.returns(EntityId.from(12476518224L), Account::id)
				.returns("john.doe@konfigyr.com", Account::email)
				.returns(AccountStatus.SUSPENDED, Account::status)
				.returns(null, Account::firstName)
				.returns(null, Account::lastName)
				.returns("john.doe@konfigyr.com", Account::displayName)
				.returns(null, Account::fullName)
				.returns(Avatar.generate(EntityId.from(12476518224L), "J"), Account::avatar)
				.returns(true, Account::isDeletable)
				.returns(Memberships.empty(), Account::memberships)
				.returns(null, Account::lastLoginAt)
				.returns(null, Account::createdAt)
				.returns(null, Account::updatedAt);
	}

	@Test
	@DisplayName("should create membership using fluent builder")
	void shouldCreateMembership() {
		final var membership = Membership.builder()
				.id(65L)
				.role(NamespaceRole.USER)
				.namespace("konfigyr")
				.name("Konfigyr")
				.avatar("https://example.com/avatar.gif")
				.since(Instant.now())
				.build();

		assertThat(membership)
				.returns(EntityId.from(65), Membership::id)
				.returns(NamespaceRole.USER, Membership::role)
				.returns("konfigyr", Membership::namespace)
				.returns("Konfigyr", Membership::name)
				.returns(Avatar.parse("https://example.com/avatar.gif"), Membership::avatar)
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
				.role(NamespaceRole.ADMIN)
				.namespace("paul.atreides")
				.name("Paul Atreides")
				.since(Instant.now())
				.build();

		final var atreides = Membership.builder()
				.id(22L)
				.role(NamespaceRole.ADMIN)
				.namespace("atreides")
				.name("Atreides")
				.since(Instant.now().plusSeconds(60))
				.build();

		final var freemen = Membership.builder()
				.id(13L)
				.role(NamespaceRole.USER)
				.namespace("freemen")
				.name("Freemen")
				.since(Instant.now().plusSeconds(180))
				.build();

		final var empire = Membership.builder()
				.id(2L)
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
				.returns("paul.atreides, atreides, freemen, empire", Memberships::join);

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
				.hasMessageContaining("Namespace role can not be null");

		assertThatThrownBy(() -> builder.role("USER").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace slug can not be blank");

		assertThatThrownBy(() -> builder.namespace("konfigyr").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace name can not be blank");

		assertThat(builder.name("Konfigyr").build())
				.returns(EntityId.from(5), Membership::id)
				.returns(NamespaceRole.USER, Membership::role)
				.returns("konfigyr", Membership::namespace)
				.returns("Konfigyr", Membership::name)
				.returns(Avatar.generate("konfigyr", "K"), Membership::avatar)
				.returns(null, Membership::since);
	}

}
