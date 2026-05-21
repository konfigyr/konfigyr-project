package com.konfigyr.membership.controller;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountManager;
import com.konfigyr.account.AccountStatus;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.feature.FeatureValue;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkRelation;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.membership.Invitation;
import com.konfigyr.membership.InvitationException;
import com.konfigyr.namespace.NamespaceFeatures;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.TestPrincipals;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class InvitationsControllerTest extends AbstractMembershipControllerTest {

	@Autowired
	AccountManager accounts;

	@Autowired
	DSLContext context;

	@Test
	@DisplayName("should retrieve invitations for namespace")
	void listInvitations() {
		mvc.get().uri("/namespaces/{slug}/invitations", "konfigyr")
				.queryParam("sort", "name")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(Invitation.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(2)
						.extracting(Invitation::key, Invitation::organization, Invitation::recipient, Invitation::role)
						.containsExactly(
								tuple(
										"09320f6c6481c1fed73573a5430758f1",
										new Invitation.Organization(EntityId.from(2), "konfigyr", "Konfigyr", "Konfigyr namespace"),
										new Invitation.Recipient("expiring@konfigyr.com"),
										NamespaceRole.USER
								),
								tuple(
										"09320d7f8e21143b2957f1caded74cbc",
										new Invitation.Organization(EntityId.from(2), "konfigyr", "Konfigyr", "Konfigyr namespace"),
										new Invitation.Recipient("invitee@konfigyr.com"),
										NamespaceRole.ADMIN
								)
						)
				)
				.satisfies(it -> assertThat(it.getMetadata())
						.returns(20L, PagedModel.PageMetadata::size)
						.returns(1L, PagedModel.PageMetadata::number)
						.returns(2L, PagedModel.PageMetadata::totalElements)
						.returns(1L, PagedModel.PageMetadata::totalPages)
				)
				.satisfies(it -> assertThat(it.getLinks())
						.hasSize(2)
						.containsExactly(
								Link.of("http://localhost?page=1", LinkRelation.FIRST),
								Link.of("http://localhost?page=1", LinkRelation.LAST)
						)
				);
	}

	@Test
	@DisplayName("should fail to list invitations for namespace when principal is not an administrator")
	void listInvitationsForNonAdministrators() {
		mvc.get().uri("/namespaces/{slug}/invitations", "konfigyr")
				.with(authentication(TestPrincipals.jane(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to list invitations for namespace when namespaces:invite scope is not present")
	void listInvitationsWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/invitations", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.INVITE_MEMBERS));
	}

	@Test
	@DisplayName("should retrieve pending invitation for namespace by key")
	void retrieveInvitation() {
		mvc.get().uri("/namespaces/{slug}/invitations/{key}", "konfigyr", "09320f6c6481c1fed73573a5430758f1")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(Invitation.class)
				.returns("09320f6c6481c1fed73573a5430758f1", Invitation::key)
				.returns(new Invitation.Organization(EntityId.from(2), "konfigyr", "Konfigyr", "Konfigyr namespace"), Invitation::organization)
				.returns(null, Invitation::sender)
				.returns(new Invitation.Recipient("expiring@konfigyr.com"), Invitation::recipient)
				.returns(NamespaceRole.USER, Invitation::role)
				.returns(true, Invitation::isExpired)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.expiryDate())
						.isCloseTo(OffsetDateTime.now().minusDays(10), within(1, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should retrieve invitation for namespace by unknown key")
	void retrieveInvitationByUnknownKey() {
		mvc.get().uri("/namespaces/{slug}/invitations/{key}", "konfigyr", "unknown")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(invitationNotFound("unknown"));
	}

	@Test
	@DisplayName("should retrieve invitation for namespace by unknown namespace")
	void retrieveInvitationByUnknownNamespace() {
		mvc.get().uri("/namespaces/{slug}/invitations/{key}", "unknown", "09320f6c6481c1fed73573a5430758f1")
				.with(authentication(TestPrincipals.john(), OAuthScope.NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@DisplayName("should fail to retrieve invitation for namespace when principal is not an administrator")
	void retrieveInvitationForNonAdministrators() {
		mvc.get().uri("/namespaces/{slug}/invitations/{key}", "konfigyr", "09320f6c6481c1fed73573a5430758f1")
				.with(authentication(TestPrincipals.jane(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to retrieve invitation for namespace when namespaces:invite scope is not present")
	void retrieveInvitationWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/invitations/{key}", "konfigyr", "09320f6c6481c1fed73573a5430758f1")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.INVITE_MEMBERS));
	}

	@Test
	@Transactional
	@DisplayName("should create invitation with new recipient")
	void createInvitationForNewRecipient() {
		mvc.post().uri("/namespaces/{slug}/invitations", "konfigyr")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"recipient@konfigyr.com\",\"role\":\"USER\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(Invitation.class)
				.returns(new Invitation.Organization(EntityId.from(2), "konfigyr", "Konfigyr", "Konfigyr namespace"), Invitation::organization)
				.returns(NamespaceRole.USER, Invitation::role)
				.returns(false, Invitation::isExpired)
				.satisfies(it -> assertThat(it.sender())
						.isNotNull()
						.returns(EntityId.from(1), Invitation.Sender::id)
						.returns("john.doe@konfigyr.com", Invitation.Sender::email)
				)
				.satisfies(it -> assertThat(it.recipient())
						.isNotNull()
						.returns(null, Invitation.Recipient::id)
						.returns("recipient@konfigyr.com", Invitation.Recipient::email)
				)
				.satisfies(it -> assertThat(it.key())
						.isNotBlank()
						.isPrintable()
						.isAlphanumeric()
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				)
				.satisfies(it -> assertThat(it.expiryDate())
						.isCloseTo(OffsetDateTime.now().plusDays(7), within(1, ChronoUnit.SECONDS))
				);
	}

	@Test
	@DisplayName("should fail to create invitation for existing member")
	void createInvitationForExistingMember() {
		mvc.post().uri("/namespaces/{slug}/invitations", "konfigyr")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"john.doe@konfigyr.com\",\"role\":\"USER\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.INTERNAL_SERVER_ERROR, problem -> problem
						.hasTitle("User is already a member")
						.hasDetailContaining("This user is already a member of the namespace.")
						.hasProperty("code", InvitationException.ErrorCode.ALREADY_INVITED.name())
				));
	}

	@Test
	@DisplayName("should fail to create invitation for Namespaces when feature is not assigned")
	void createInvitationForNamespaceWithoutFeature() {
		doReturn(Optional.empty()).when(features).get("john-doe", NamespaceFeatures.MEMBERS_COUNT);

		mvc.post().uri("/namespaces/{slug}/invitations", "john-doe")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"invitee@konfigyr.com\",\"role\":\"USER\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitle("Inviting members is not allowed")
						.hasDetailContaining("This namespace does not support member invites at the moment")
						.hasProperty("code", InvitationException.ErrorCode.NOT_ALLOWED.name())
				));
	}

	@Test
	@DisplayName("should fail to create invitation for Namespaces when membership limit is reached")
	void createInvitationForNamespaceWithMembershipLimitReached() {
		doReturn(Optional.of(FeatureValue.limited(1))).when(features).get("john-doe", NamespaceFeatures.MEMBERS_COUNT);

		mvc.post().uri("/namespaces/{slug}/invitations", "john-doe")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"invitee@konfigyr.com\",\"role\":\"USER\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitle("Member limit reached")
						.hasDetailContaining("This organization has reached its maximum allowed number of members")
						.hasProperty("code", InvitationException.ErrorCode.MEMBER_LIMIT_REACHED.name())
				));
	}

	@Test
	@DisplayName("should fail to create invitation for unknown namespace")
	void createInvitationForUnknownNamespace() {
		mvc.post().uri("/namespaces/{slug}/invitations", "unknown")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"invitee@konfigyr.com\",\"role\":\"USER\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@DisplayName("should fail to create invitation for namespace when principal is not an administrator")
	void createInvitationForNonAdministrators() {
		mvc.post().uri("/namespaces/{slug}/invitations", "konfigyr")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"recipient@konfigyr.com\",\"role\":\"USER\"}")
				.with(authentication(TestPrincipals.jane(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to create invitation for namespace when namespaces:invite scope is not present")
	void createInvitationWithoutScope() {
		mvc.post().uri("/namespaces/{slug}/invitations", "konfigyr")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"recipient@konfigyr.com\",\"role\":\"USER\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.INVITE_MEMBERS));
	}

	@Test
	@DisplayName("should fail to create invitation with invalid data")
	void validateInvitation() {
		mvc.post().uri("/namespaces/{slug}/invitations", "konfigyr")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitleContaining("Invalid")
						.hasDetailContaining("invalid request data")
						.hasPropertySatisfying("errors", errors -> assertThat(errors)
								.isNotNull()
								.isInstanceOf(Collection.class)
								.asInstanceOf(InstanceOfAssertFactories.collection(Map.class))
								.extracting("pointer")
								.containsExactlyInAnyOrder("email", "role")
						)
				));
	}

	@Test
	@Transactional
	@DisplayName("should accept pending invitation")
	void acceptPendingInvitation() {
		final Account invitee = createAccount("invitee@konfigyr.com", "Paul", "Atreides");

		mvc.post().uri("/account/invitations/{key}", "09320d7f8e21143b2957f1caded74cbc")
				.with(authentication(invitee))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	@Transactional
	@DisplayName("should fail to accept expired invitation")
	void acceptExpiredInvitation() {
		final Account invitee = createAccount("expiring@konfigyr.com", "Leto", "Atreides");

		mvc.post().uri("/account/invitations/{key}", "09320f6c6481c1fed73573a5430758f1")
				.with(authentication(invitee))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitle("Invitation not found or expired")
						.hasDetailContaining("This invitation link is no longer valid.")
						.hasDetailContaining("It may have expired or was already used.")
						.hasProperty("code", InvitationException.ErrorCode.INVITATION_EXPIRED.name())
				))
				.satisfies(hasFailedWithException(InvitationException.class, ex -> ex
						.hasMessageContaining("Can not accept expiring invitations")
						.returns(InvitationException.ErrorCode.INVITATION_EXPIRED, InvitationException::getCode)
				));
	}

	@Test
	@Transactional
	@DisplayName("should fail to accept unknown invitation")
	void acceptUnknownInvitation() {
		final Account invitee = createAccount("invitee@konfigyr.com", "Paul", "Atreides");

		mvc.post().uri("/account/invitations/{key}", "unknown")
				.with(authentication(invitee))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(invitationNotFound("unknown"));
	}

	@Test
	@DisplayName("should fail to accept invitation that is not sent to the currently logged in account")
	void acceptInvitationForDifferentAccount() {
		mvc.post().uri("/account/invitations/{key}", "09320d7f8e21143b2957f1caded74cbc")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(invitationNotFound("09320d7f8e21143b2957f1caded74cbc"));
	}

	@Test
	@Transactional
	@DisplayName("should retrieve invitations for authenticated account")
	void listAccountInvitations() {
		final Account invitee = createAccount("invitee@konfigyr.com", "Paul", "Atreides");

		mvc.get().uri("/account/invitations")
				.with(authentication(invitee))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(Invitation.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(1)
						.extracting(Invitation::key, Invitation::organization, Invitation::role)
						.containsExactly(tuple(
								"09320d7f8e21143b2957f1caded74cbc",
								new Invitation.Organization(EntityId.from(2), "konfigyr", "Konfigyr", "Konfigyr namespace"),
								NamespaceRole.ADMIN
						))
				);
	}

	@Test
	@DisplayName("should retrieve empty invitations for account with no invitations")
	void listEmptyAccountInvitations() {
		mvc.get().uri("/account/invitations")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(pagedModel(Invitation.class))
				.satisfies(it -> assertThat(it.getContent()).isEmpty());
	}

	@Test
	@DisplayName("should fail to list account invitations for invalid authentication type")
	void listAccountInvitationsForInvalidAuthenticationType() {
		mvc.get().uri("/account/invitations")
				.with(authentication(TestPrincipals.application("kfg-client-application")))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@Transactional
	@DisplayName("should retrieve invitation for authenticated account by key")
	void retrieveAccountInvitation() {
		final Account invitee = createAccount("invitee@konfigyr.com", "Paul", "Atreides");

		mvc.get().uri("/account/invitations/{key}", "09320d7f8e21143b2957f1caded74cbc")
				.with(authentication(invitee))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(Invitation.class)
				.returns("09320d7f8e21143b2957f1caded74cbc", Invitation::key)
				.returns(new Invitation.Organization(EntityId.from(2), "konfigyr", "Konfigyr", "Konfigyr namespace"), Invitation::organization)
				.returns(NamespaceRole.ADMIN, Invitation::role)
				.returns(false, Invitation::isExpired)
				.returns(new Invitation.Recipient("invitee@konfigyr.com"), Invitation::recipient);
	}

	@Test
	@Transactional
	@DisplayName("should fail to retrieve account invitation by unknown key")
	void retrieveUnknownAccountInvitation() {
		final Account invitee = createAccount("invitee@konfigyr.com", "Paul", "Atreides");

		mvc.get().uri("/account/invitations/{key}", "unknown")
				.with(authentication(invitee))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(invitationNotFound("unknown"));
	}

	@Test
	@DisplayName("should fail to retrieve account invitation that is not sent to the currently logged in account")
	void retrieveAccountInvitationForDifferentAccount() {
		mvc.get().uri("/account/invitations/{key}", "09320d7f8e21143b2957f1caded74cbc")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(invitationNotFound("09320d7f8e21143b2957f1caded74cbc"));
	}

	@Test
	@Transactional
	@DisplayName("should decline pending account invitation")
	void declineAccountInvitation() {
		final Account invitee = createAccount("invitee@konfigyr.com", "Paul", "Atreides");

		mvc.delete().uri("/account/invitations/{key}", "09320d7f8e21143b2957f1caded74cbc")
				.with(authentication(invitee))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	@Transactional
	@DisplayName("should fail to decline unknown account invitation")
	void declineUnknownAccountInvitation() {
		final Account invitee = createAccount("invitee@konfigyr.com", "Paul", "Atreides");

		mvc.delete().uri("/account/invitations/{key}", "unknown")
				.with(authentication(invitee))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(invitationNotFound("unknown"));
	}

	@Test
	@DisplayName("should fail to decline account invitation that is not sent to the currently logged in account")
	void declineAccountInvitationForDifferentAccount() {
		mvc.delete().uri("/account/invitations/{key}", "09320d7f8e21143b2957f1caded74cbc")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(invitationNotFound("09320d7f8e21143b2957f1caded74cbc"));
	}

	@Test
	@Transactional
	@DisplayName("should cancel pending invitation for namespace")
	void cancelPendingInvitation() {
		mvc.delete().uri("/namespaces/{slug}/invitations/{key}", "konfigyr", "09320d7f8e21143b2957f1caded74cbc")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	@Transactional
	@DisplayName("should cancel expiring invitation for namespace")
	void cancelExpiringInvitation() {
		mvc.delete().uri("/namespaces/{slug}/invitations/{key}", "konfigyr", "09320f6c6481c1fed73573a5430758f1")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	@DisplayName("should cancel unknown invitation for namespace")
	void cancelUnknownInvitation() {
		mvc.delete().uri("/namespaces/{slug}/invitations/{key}", "konfigyr", "unknown")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(invitationNotFound("unknown"));
	}

	@Test
	@DisplayName("should fail to cancel invitation without namespaces:invite scope")
	void cancelInvitationsWithoutScope() {
		mvc.delete().uri("/namespaces/{slug}/invitations/{key}", "konfigyr", "09320f6c6481c1fed73573a5430758f1")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.INVITE_MEMBERS));
	}

	private Account createAccount(String email, String firstName, String lastName) {
		return context.insertInto(ACCOUNTS)
				.set(
						SettableRecord.of(ACCOUNTS)
								.set(ACCOUNTS.ID, EntityId.generate().map(EntityId::get))
								.set(ACCOUNTS.EMAIL, email)
								.set(ACCOUNTS.STATUS, AccountStatus.ACTIVE.name())
								.set(ACCOUNTS.FIRST_NAME, firstName)
								.set(ACCOUNTS.LAST_NAME, lastName)
								.get()
				)
				.returning(ACCOUNTS.ID)
				.fetchOptional(ACCOUNTS.ID)
				.map(EntityId::from)
				.flatMap(accounts::findById)
				.orElseThrow(() -> new IllegalStateException("Failed to create test account: " + email));
	}

	static Consumer<MvcTestResult> invitationNotFound(String key) {
		return problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
				.hasTitle("Invitation not found or expired")
				.hasDetailContaining("This invitation link is no longer valid.")
				.hasDetailContaining("It may have expired or was already used.")
				.hasProperty("code", InvitationException.ErrorCode.INVITATION_NOT_FOUND.name())
		).andThen(hasFailedWithException(InvitationException.class, ex -> ex
				.returns(InvitationException.ErrorCode.INVITATION_NOT_FOUND, InvitationException::getCode)
				.hasMessageContaining("Invitation with key \"%s\" not found".formatted(key))
		));
	}

}
