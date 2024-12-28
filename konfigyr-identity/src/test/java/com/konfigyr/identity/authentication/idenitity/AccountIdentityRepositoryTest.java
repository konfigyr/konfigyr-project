package com.konfigyr.identity.authentication.idenitity;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.TestClients;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.AccountIdentityEvent;
import com.konfigyr.identity.authentication.AccountIdentityExistsException;
import com.konfigyr.identity.authentication.AccountIdentityStatus;
import com.konfigyr.support.Avatar;
import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.modulith.test.PublishedEventsExtension;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestProfile
@SpringBootTest
@ExtendWith(PublishedEventsExtension.class)
@ImportTestcontainers(TestContainers.class)
class AccountIdentityRepositoryTest {

	@Autowired
	AccountIdentityRepository repository;

	@Test
	@DisplayName("should lookup account identity by entity identifier")
	void shouldLookupIdentityById() {
		final var id = EntityId.from(1);

		assertThat(repository.findById(id))
				.isPresent()
				.get()
				.returns(id, AccountIdentity::getId)
				.returns(id.serialize(), AccountIdentity::getName)
				.returns(id.serialize(), AccountIdentity::getUsername)
				.returns(AccountIdentityStatus.ACTIVE, AccountIdentity::getStatus)
				.returns("john.doe@konfigyr.com", AccountIdentity::getEmail)
				.returns("John Doe", AccountIdentity::getDisplayName)
				.returns(Avatar.generate(id, "JD"), AccountIdentity::getAvatar);
	}

	@Test
	@DisplayName("should lookup account identity by email address")
	void shouldLookupIdentityByEmail() {
		final var email = "jane.doe@konfigyr.com";

		assertThat(repository.findByEmail(email))
				.isPresent()
				.get()
				.returns(EntityId.from(2), AccountIdentity::getId)
				.returns(EntityId.from(2).serialize(), AccountIdentity::getName)
				.returns(EntityId.from(2).serialize(), AccountIdentity::getUsername)
				.returns(AccountIdentityStatus.ACTIVE, AccountIdentity::getStatus)
				.returns("jane.doe@konfigyr.com", AccountIdentity::getEmail)
				.returns("Jane Doe", AccountIdentity::getDisplayName)
				.returns(Avatar.generate(EntityId.from(2), "JD"), AccountIdentity::getAvatar);
	}

	@Test
	@DisplayName("should return empty optional when account identity is not found by entity identifier")
	void shouldFailToLookupIdentityById() {
		assertThat(repository.findById(EntityId.from(991827464))).isEmpty();
	}


	@Test
	@DisplayName("should return empty optional when account identity is not found by email address")
	void shouldFailToLookupIdentityByEmail() {
		assertThat(repository.findByEmail("unknown@konfigyr.com")).isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should create account identity from Github OAuth2 user")
	void shouldCreateIdentityFromGithubOAuth2User(AssertablePublishedEvents events) {
		final var principal = new DefaultOAuth2User(null, Map.of(
				"email", "muad.dib@arakis.com",
				"name", "Paul Atreides",
				"avatar_url", "https://i.pinimg.com/originals/53/32/87/53328770473d527d3400355e9d0edb15.jpg"
		), "email");

		final var identity = repository.create(principal, TestClients.clientRegistration("github").build());

		assertThat(identity)
				.returns(AccountIdentityStatus.ACTIVE, AccountIdentity::getStatus)
				.returns("muad.dib@arakis.com", AccountIdentity::getEmail)
				.returns("Paul Atreides", AccountIdentity::getDisplayName)
				.returns(Avatar.parse("https://i.pinimg.com/originals/53/32/87/53328770473d527d3400355e9d0edb15.jpg"), AccountIdentity::getAvatar)
				.satisfies(it -> assertThat(it.getId())
						.isNotNull()
						.extracting(EntityId::serialize)
						.isEqualTo(it.getName())
						.isEqualTo(it.getUsername())
				);

		events.ofType(AccountIdentityEvent.Created.class)
				.matching(AccountIdentityEvent::identity, identity);
	}

	@Test
	@Transactional
	@DisplayName("should create account identity from Gitlab OAuth2 user")
	void shouldCreateIdentityFromGitlabOAuth2User(AssertablePublishedEvents events) {
		final var principal = new DefaultOAuth2User(null, Map.of(
				"email", "muad.dib@arakis.com"
		), "email");

		final var identity = repository.create(principal, TestClients.clientRegistration("gitlab").build());

		assertThat(identity)
				.returns(AccountIdentityStatus.ACTIVE, AccountIdentity::getStatus)
				.returns("muad.dib@arakis.com", AccountIdentity::getEmail)
				.returns("muad.dib@arakis.com", AccountIdentity::getDisplayName)
				.satisfies(it -> assertThat(it.getId())
						.isNotNull()
						.extracting(EntityId::serialize)
						.isEqualTo(it.getName())
						.isEqualTo(it.getUsername())
				)
				.satisfies(it -> assertThat(it.getAvatar())
						.isNotNull()
						.isEqualTo(Avatar.generate(it.getName(), " M"))
				);

		events.ofType(AccountIdentityEvent.Created.class)
				.matching(AccountIdentityEvent::identity, identity);
	}

	@Test
	@Transactional
	@DisplayName("should create account identity from OpenID user")
	void shouldCreateIdentityFromOpenIDUser(AssertablePublishedEvents events) {
		final var principal = new DefaultOAuth2User(null, Map.of(
				"email", "muad.dib@arakis.com",
				"name", "Paul Atreides",
				"picture", "https://i.pinimg.com/originals/53/32/87/53328770473d527d3400355e9d0edb15.jpg"
		), "email");

		final var identity = repository.create(principal, TestClients.clientRegistration("google").build());

		assertThat(identity)
				.returns(AccountIdentityStatus.ACTIVE, AccountIdentity::getStatus)
				.returns("muad.dib@arakis.com", AccountIdentity::getEmail)
				.returns("Paul Atreides", AccountIdentity::getDisplayName)
				.returns(Avatar.parse("https://i.pinimg.com/originals/53/32/87/53328770473d527d3400355e9d0edb15.jpg"), AccountIdentity::getAvatar)
				.satisfies(it -> assertThat(it.getId())
						.isNotNull()
						.extracting(EntityId::serialize)
						.isEqualTo(it.getName())
						.isEqualTo(it.getUsername())
				);

		events.ofType(AccountIdentityEvent.Created.class)
				.matching(AccountIdentityEvent::identity, identity);
	}

	@Test
	@DisplayName("should fail to create account identity with existing email address")
	void shouldNotCreateIdentityWithExistingEmailAddress(AssertablePublishedEvents events) {
		final var principal = new DefaultOAuth2User(null, Map.of(
				"email", "jane.doe@konfigyr.com"
		), "email");

		assertThatThrownBy(() -> repository.create(principal, TestClients.clientRegistration("google").build()))
				.isInstanceOf(AccountIdentityExistsException.class)
				.extracting("user")
				.isEqualTo(principal);

		assertThat(events.eventOfTypeWasPublished(AccountIdentityEvent.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to create account identity without email address")
	void shouldNotCreateIdentityWithoutEmailAddress(AssertablePublishedEvents events) {
		final var principal = new DefaultOAuth2User(null, Map.of("id", "unknown"), "id");

		assertThatThrownBy(() -> repository.create(principal, TestClients.clientRegistration("google").build()))
				.isInstanceOf(InternalAuthenticationServiceException.class);

		assertThat(events.eventOfTypeWasPublished(AccountIdentityEvent.class))
				.isFalse();
	}

}
