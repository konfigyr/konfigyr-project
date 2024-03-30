package com.konfigyr.account;

import com.konfigyr.NamespaceTestConfiguration;
import com.konfigyr.entity.EntityId;
import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.modulith.test.PublishedEventsExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestProfile
@ImportTestcontainers(TestContainers.class)
@SpringBootTest(classes = NamespaceTestConfiguration.class)
@ExtendWith(PublishedEventsExtension.class)
class AccountManagerTest {

	@Autowired
	AccountManager manager;

	@Test
	@DisplayName("should lookup account by entity identifier")
	void shouldLookupAccountById() {
		final var id = EntityId.from(1);

		assertThat(manager.findById(id))
				.isPresent()
				.get()
				.returns(id, Account::id)
				.returns(AccountStatus.ACTIVE, Account::status)
				.returns("john.doe@konfigyr.com", Account::email)
				.returns("John", Account::firstName)
				.returns("Doe", Account::lastName)
				.returns("John Doe", Account::displayName)
				.returns(null, Account::avatar)
				.satisfies(it -> assertThat(it.lastLoginAt())
						.isNotNull()
						.isEqualToIgnoringMinutes(OffsetDateTime.now())
				)
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.satisfies(it -> assertThat(it.updatedAt()).isNotNull());
	}

	@Test
	@DisplayName("should lookup account by email address")
	void shouldLookupAccountByEmail() {
		final var email = "jane.doe@konfigyr.com";

		assertThat(manager.findByEmail(email))
				.isPresent()
				.get()
				.returns(EntityId.from(2), Account::id)
				.returns(AccountStatus.INITIAL, Account::status)
				.returns(email, Account::email)
				.returns("Jane", Account::firstName)
				.returns("Doe", Account::lastName)
				.returns("Jane Doe", Account::displayName)
				.returns(null, Account::avatar)
				.returns(null, Account::lastLoginAt)
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.satisfies(it -> assertThat(it.updatedAt()).isNotNull());
	}

	@Test
	@DisplayName("should return empty optional when account is not found by entity identifier")
	void shouldFailToLookupAccountById() {
		assertThat(manager.findById(EntityId.from(991827464))).isEmpty();
	}

	@Test
	@DisplayName("should return empty optional when account is not found by email address")
	void shouldFailToLookupAccountByEmail() {
		assertThat(manager.findByEmail("unknown@konfigyr.com")).isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should create account with registration data")
	void shouldRegisterAccount() {
		final var registration = AccountRegistration.builder()
				.email("muad.dib@arakis.com")
				.firstName("Paul")
				.lastName("Atreides")
				.avatar("https://i.pinimg.com/originals/53/32/87/53328770473d527d3400355e9d0edb15.jpg")
				.build();

		assertThat(manager.create(registration))
				.returns(AccountStatus.INITIAL, Account::status)
				.returns("muad.dib@arakis.com", Account::email)
				.returns("Paul", Account::firstName)
				.returns("Atreides", Account::lastName)
				.returns("Paul Atreides", Account::displayName)
				.returns("https://i.pinimg.com/originals/53/32/87/53328770473d527d3400355e9d0edb15.jpg", Account::avatar)
				.returns(null, Account::lastLoginAt)
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.satisfies(it -> assertThat(it.updatedAt()).isNotNull());
	}

	@Test
	@Transactional
	@DisplayName("should create account with registration data containing only an email address")
	void shouldRegisterAccountWithJustEmail(PublishedEvents events) {
		final var registration = AccountRegistration.builder()
						.email("muad.dib@arakis.com")
						.build();

		assertThat(manager.create(registration))
				.returns(AccountStatus.INITIAL, Account::status)
				.returns("muad.dib@arakis.com", Account::email)
				.returns(null, Account::firstName)
				.returns(null, Account::lastName)
				.returns("muad.dib@arakis.com", Account::displayName)
				.returns(null, Account::avatar)
				.returns(null, Account::lastLoginAt)
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.satisfies(it -> assertThat(it.updatedAt()).isNotNull());

		events.eventOfTypeWasPublished(AccountRegisteredEvent.class);
	}

	@Test
	@Transactional
	@DisplayName("should create account with registration data with full name and email address")
	void shouldRegisterAccountWithFullName(PublishedEvents events) {
		final var registration = AccountRegistration.builder()
				.email("muad.dib@arakis.com")
				.fullName("Paul Atreides")
				.build();

		assertThat(manager.create(registration))
				.returns(AccountStatus.INITIAL, Account::status)
				.returns("muad.dib@arakis.com", Account::email)
				.returns("Paul", Account::firstName)
				.returns("Atreides", Account::lastName)
				.returns("Paul Atreides", Account::displayName)
				.returns(null, Account::avatar)
				.returns(null, Account::lastLoginAt)
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.satisfies(it -> assertThat(it.updatedAt()).isNotNull());

		events.eventOfTypeWasPublished(AccountRegisteredEvent.class);
	}

	@Test
	@DisplayName("should fail to create account with existing email address")
	void shouldNotRegisterAccountWithSameEmail(PublishedEvents events) {
		final var registration = AccountRegistration.builder()
				.email("jane.doe@konfigyr.com")
				.build();

		assertThatThrownBy(() -> manager.create(registration))
				.isInstanceOf(AccountExistsException.class)
				.hasCauseInstanceOf(DuplicateKeyException.class);

		assertThat(events.ofType(AccountRegisteredEvent.class))
				.isEmpty();
	}

}