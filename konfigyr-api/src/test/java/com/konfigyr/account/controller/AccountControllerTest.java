package com.konfigyr.account.controller;

import com.google.gson.JsonParser;
import com.konfigyr.account.Account;
import com.konfigyr.account.AccountEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.mail.Mail;
import com.konfigyr.support.Avatar;
import com.konfigyr.support.FullName;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.modulith.test.PublishedEventsExtension;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

@ExtendWith(PublishedEventsExtension.class)
class AccountControllerTest extends AbstractControllerTest {

	@Test
	@DisplayName("retrieve currently logged in user account details")
	void retrieveAccountDetails() {
		mvc.get().uri("/account")
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(Account.class)
				.returns(EntityId.from(2), Account::id)
				.returns("jane.doe@konfigyr.com", Account::email)
				.returns(Avatar.generate(EntityId.from(2), "JD"), Account::avatar)
				.returns(FullName.of("Jane", "Doe"), Account::fullName)
				.returns("Jane", Account::firstName)
				.returns("Doe", Account::lastName)
				.returns(true, Account::isDeletable);
	}

	@Test
	@DisplayName("fail to retrieve unknown user account details")
	void unknownRetrieve() {
		mvc.get().uri("/account")
				.with(authentication(EntityId.from(9999).serialize()))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DisplayName("fail to retrieve user account details when authentication principal is invalid")
	void invalidPrincipal() {
		mvc.get().uri("/account")
				.with(authentication("invalid-entity-identifier"))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DisplayName("fail to retrieve user account details when authentication is not present")
	void unauthorizedRetrieve() {
		mvc.get().uri("/account")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@Transactional
	@DisplayName("should change account email address")
	void updateEmailAddress(AssertablePublishedEvents events) throws Exception {
		doNothing().when(mailer).send(any());

		final var result = mvc.post().uri("/account/email")
				.with(authentication(TestPrincipals.jane()))
				.characterEncoding(StandardCharsets.UTF_8)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"jane-doe@konfigyr.com\"}")
				.exchange();

		result.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.hasPathSatisfying("$.token", it -> it.assertThat()
						.asInstanceOf(InstanceOfAssertFactories.STRING)
						.isNotBlank()
				)
				.hasPathSatisfying("$.email", it -> it.assertThat()
						.asInstanceOf(InstanceOfAssertFactories.STRING)
						.isEqualTo("jane-doe@konfigyr.com")
				);

		final var captor = ArgumentCaptor.forClass(Mail.class);
		verify(mailer).send(captor.capture());

		final var json = JsonParser.parseString(result.getResponse().getContentAsString()).getAsJsonObject();
		json.addProperty("code", String.valueOf(captor.getValue().attributes().get("code")));
		json.remove("email");

		mvc.put().uri("/account/email")
				.with(authentication(TestPrincipals.jane()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.toString())
				.exchange()
				.assertThat()
				.apply(log())
				.bodyJson()
				.convertTo(Account.class)
				.returns(EntityId.from(2), Account::id)
				.returns("jane-doe@konfigyr.com", Account::email);

		assertThat(events.eventOfTypeWasPublished(AccountEvent.Updated.class))
				.isTrue();
	}

	@Test
	@DisplayName("should fail to issue email address verification token for invalid email address")
	void verifyEmailInvalid() {
		mvc.post().uri("/account/email")
				.with(authentication(TestPrincipals.jane()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"invalid-email-address\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasPropertySatisfying("errors", error -> assertThat(error)
								.asInstanceOf(InstanceOfAssertFactories.iterable(Map.class))
								.extracting("pointer")
								.containsExactly("email")
						)
				));
	}

	@Test
	@DisplayName("should fail to issue email address verification token for unknown account")
	void verifyEmailUnknown() {
		mvc.post().uri("/account/email")
				.with(authentication(EntityId.from(9999).serialize()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"jane-doe@konfigyr.com\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DisplayName("should fail to update email address for invalid request")
	void updateEmailInvalid() {
		mvc.put().uri("/account/email")
				.with(authentication(TestPrincipals.jane()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasPropertySatisfying("errors", error -> assertThat(error)
								.asInstanceOf(InstanceOfAssertFactories.iterable(Map.class))
								.extracting("pointer")
								.containsExactlyInAnyOrder("code", "token")
						)
				));
	}

	@Test
	@DisplayName("should fail to update email address for unknown account")
	void updateEmailUnknown() {
		mvc.put().uri("/account/email")
				.with(authentication(EntityId.from(9999).serialize()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"token\",\"code\":123456}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@Transactional
	@DisplayName("update currently logged in user account")
	void updateAccount(AssertablePublishedEvents events) {
		mvc.patch().uri("/account")
				.with(authentication(TestPrincipals.jane()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Jane Fonda\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(Account.class)
				.returns(EntityId.from(2), Account::id)
				.returns("jane.doe@konfigyr.com", Account::email)
				.returns(FullName.of("Jane", "Fonda"), Account::fullName)
				.returns("Jane", Account::firstName)
				.returns("Fonda", Account::lastName);

		assertThat(events.eventOfTypeWasPublished(AccountEvent.Updated.class))
				.isTrue();
	}

	@Test
	@DisplayName("should not update user account when invalid data is specified")
	void invalidUpdate(AssertablePublishedEvents events) {
		mvc.patch().uri("/account")
				.with(authentication(TestPrincipals.jane()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"   \"}")
				.exchange()
				.assertThat()
				.apply(log())
				.bodyJson()
				.convertTo(Account.class)
				.returns("jane.doe@konfigyr.com", Account::email)
				.returns(FullName.of("Jane", "Doe"), Account::fullName)
				.returns("Jane", Account::firstName)
				.returns("Doe", Account::lastName);

		assertThat(events.eventOfTypeWasPublished(AccountEvent.Updated.class))
				.isFalse();
	}

	@Test
	@DisplayName("fail to update unknown user account")
	void unknownUpdate(AssertablePublishedEvents events) {
		mvc.patch().uri("/account")
				.with(authentication(EntityId.from(9999).serialize()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Jane Fonda\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.UNAUTHORIZED);

		assertThat(events.eventOfTypeWasPublished(AccountEvent.Updated.class))
				.isFalse();
	}

	@Test
	@DisplayName("fail to update user account when authentication is not present")
	void unauthorizedUpdate() {
		mvc.patch().uri("/account")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@Transactional
	@DisplayName("delete currently logged in user account")
	void deleteAccount(AssertablePublishedEvents events) {
		mvc.delete().uri("/account")
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NO_CONTENT);

		assertThat(events.eventOfTypeWasPublished(AccountEvent.Deleted.class))
				.isTrue();
	}

	@Test
	@DisplayName("fail to delete unknown user account")
	void unknownDelete(AssertablePublishedEvents events) {
		mvc.delete().uri("/account")
				.with(authentication(EntityId.from(9999).serialize()))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.UNAUTHORIZED);

		assertThat(events.eventOfTypeWasPublished(AccountEvent.Updated.class))
				.isFalse();
	}

	@Test
	@DisplayName("fail to delete user account when authentication is not present")
	void unauthorizedDelete() {
		mvc.delete().uri("/account")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

}
