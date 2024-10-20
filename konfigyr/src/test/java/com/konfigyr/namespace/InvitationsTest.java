package com.konfigyr.namespace;

import com.konfigyr.account.AccountStatus;
import com.konfigyr.entity.EntityId;
import com.konfigyr.jooq.SettableRecord;
import com.konfigyr.mail.Mail;
import com.konfigyr.support.FullName;
import com.konfigyr.test.AbstractIntegrationTest;
import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.modulith.test.PublishedEventsExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static com.konfigyr.data.tables.Accounts.ACCOUNTS;

@ExtendWith(PublishedEventsExtension.class)
class InvitationsTest extends AbstractIntegrationTest {

	@Autowired
	NamespaceManager namespaces;

	@Autowired
	Invitations invitations;

	@Autowired
	DSLContext context;

	@Test
	@Transactional
	@DisplayName("should create invitation and send invitation email to recipient")
	void shouldCreateAndSendInvitation(AssertablePublishedEvents events) {
		final var invite = new Invite(
				EntityId.from(2),
				EntityId.from(1),
				"recipient@konfigyr.com",
				NamespaceRole.USER
		);

		final var invitation = invitations.create(invite);

		assertThat(invitation)
				.isNotNull()
				.returns(invite.role(), Invitation::role)
				.satisfies(it -> assertThat(it.key())
						.isNotBlank()
						.hasSize(16)
						.isAlphanumeric()
						.isPrintable()
				)
				.satisfies(it -> assertThat(it.recipient())
						.returns(invite.recipient(), Invitation.Recipient::email)
						.returns(false, Invitation.Recipient::exists)
						.returns(null, Invitation.Recipient::id)
						.returns(null, Invitation.Recipient::name)
				)
				.satisfies(it -> assertThat(it.sender())
						.isNotNull()
						.returns(EntityId.from(1), Invitation.Sender::id)
						.returns("john.doe@konfigyr.com", Invitation.Sender::email)
						.returns(FullName.of("John", "Doe"), Invitation.Sender::name)
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				)
				.satisfies(it -> assertThat(it.expiryDate())
						.isCloseTo(OffsetDateTime.now().plusDays(7), within(1, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(InvitationEvent.Created.class)
				.matching(event -> invitation.namespace().equals(event.id()))
				.matching(event -> invitation.namespace().equals(event.namespace()))
				.matching(event -> invitation.key().equals(event.key()));
	}

	@Test
	@Transactional
	@DisplayName("should create invitation to an existing account and send invitation email to recipient")
	void shouldCreateAndSendInvitationToExistingAccount(AssertablePublishedEvents events) {
		final var account = createAccount("peter.vries@arakis.com", "Piter", "De Vries");

		final var invite = new Invite(
				EntityId.from(2),
				EntityId.from(1),
				"peter.vries@arakis.com",
				NamespaceRole.USER
		);

		final var invitation = invitations.create(invite);

		assertThat(invitation)
				.isNotNull()
				.returns(invite.role(), Invitation::role)
				.satisfies(it -> assertThat(it.key())
						.isNotBlank()
						.hasSize(16)
						.isAlphanumeric()
						.isPrintable()
				)
				.satisfies(it -> assertThat(it.recipient())
						.returns(invite.recipient(), Invitation.Recipient::email)
						.returns(true, Invitation.Recipient::exists)
						.returns(account, Invitation.Recipient::id)
						.returns(FullName.of("Piter", "De Vries"), Invitation.Recipient::name)
				)
				.satisfies(it -> assertThat(it.sender())
						.isNotNull()
						.returns(EntityId.from(1), Invitation.Sender::id)
						.returns("john.doe@konfigyr.com", Invitation.Sender::email)
						.returns(FullName.of("John", "Doe"), Invitation.Sender::name)
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				)
				.satisfies(it -> assertThat(it.expiryDate())
						.isCloseTo(OffsetDateTime.now().plusDays(7), within(1, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(InvitationEvent.Created.class)
				.matching(event -> invitation.namespace().equals(event.id()))
				.matching(event -> invitation.namespace().equals(event.namespace()))
				.matching(event -> invitation.key().equals(event.key()));
	}

	@Test
	@DisplayName("should fail to create invitation for account without permissions")
	void shouldFailToCreateInvitationForInsufficientPermissions(AssertablePublishedEvents events) {
		final var invite = new Invite(
				EntityId.from(2),
				EntityId.from(2),
				"recipient@konfigyr.com",
				NamespaceRole.USER
		);

		assertThatThrownBy(() -> invitations.create(invite))
				.isInstanceOf(InvitationException.class)
				.extracting("code")
				.isEqualTo(InvitationException.ErrorCode.INSUFFICIENT_PERMISSIONS);

		Assertions.assertThat(events.ofType(Mail.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to create invitation for a recipient that is already a member")
	void shouldFailToCreateInvitationForExistingMember(AssertablePublishedEvents events) {
		final var invite = new Invite(
				EntityId.from(2),
				EntityId.from(1),
				"john.doe@konfigyr.com",
				NamespaceRole.ADMIN
		);

		assertThatThrownBy(() -> invitations.create(invite))
				.isInstanceOf(InvitationException.class)
				.extracting("code")
				.isEqualTo(InvitationException.ErrorCode.ALREADY_INVITED);

		Assertions.assertThat(events.ofType(Mail.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to create invitation for personal namespaces")
	void shouldFailToCreateInvitationForPersonalNamespaces(AssertablePublishedEvents events) {
		final var invite = new Invite(
				EntityId.from(1),
				EntityId.from(1),
				"recipient@konfigyr.com",
				NamespaceRole.USER
		);

		assertThatThrownBy(() -> invitations.create(invite))
				.isInstanceOf(InvitationException.class)
				.extracting("code")
				.isEqualTo(InvitationException.ErrorCode.UNSUPPORTED_NAMESPACE_TYPE);

		Assertions.assertThat(events.ofType(Mail.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to create invitation for unknown namespace")
	void shouldFailToCreateInvitationForUnknownNamespace(AssertablePublishedEvents events) {
		final var invite = new Invite(
				EntityId.from(9999),
				EntityId.from(1),
				"recipient@konfigyr.com",
				NamespaceRole.USER
		);

		assertThatThrownBy(() -> invitations.create(invite))
				.isInstanceOf(InvitationException.class)
				.extracting("code")
				.isEqualTo(InvitationException.ErrorCode.INSUFFICIENT_PERMISSIONS);

		Assertions.assertThat(events.ofType(Mail.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve all invitations for namespace")
	void shouldRetrieveInvitations() {
		final var namespace = namespaces.findBySlug("konfigyr");
		assertThat(namespace).isPresent();

		assertThat(invitations.find(namespace.get(), Pageable.unpaged()))
				.hasSize(2)
				.extracting(Invitation::key)
				.containsExactlyInAnyOrder("qT6uq2ZP1Yv2bWmt", "B1LPctaRXp6sxRo7");
	}

	@Test
	@DisplayName("should retrieve no invitations for namespace that has no invitations")
	void shouldRetrieveEmptyInvitations() {
		final var namespace = namespaces.findBySlug("john-doe");
		assertThat(namespace).isPresent();

		assertThat(invitations.find(namespace.get(), Pageable.unpaged()))
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve invitation by namespace and key")
	void shouldRetrieveInvitation() {
		final var namespace = namespaces.findBySlug("konfigyr");
		assertThat(namespace).isPresent();

		assertThat(invitations.get(namespace.get(), "qT6uq2ZP1Yv2bWmt"))
				.isPresent()
				.get()
				.returns("qT6uq2ZP1Yv2bWmt", Invitation::key)
				.returns(namespace.get().id(), Invitation::namespace)
				.returns(NamespaceRole.ADMIN, Invitation::role)
				.returns(false, Invitation::isExpired)
				.satisfies(it -> assertThat(it.recipient())
						.returns("invitee@konfigyr.com", Invitation.Recipient::email)
						.returns(false, Invitation.Recipient::exists)
						.returns(null, Invitation.Recipient::id)
						.returns(null, Invitation.Recipient::name)
				)
				.satisfies(it -> assertThat(it.sender())
						.isNotNull()
						.returns(EntityId.from(1), Invitation.Sender::id)
						.returns("john.doe@konfigyr.com", Invitation.Sender::email)
						.returns(FullName.of("John", "Doe"), Invitation.Sender::name)
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.expiryDate())
						.isCloseTo(
								OffsetDateTime.now().plusDays(7).withOffsetSameLocal(it.expiryDate().getOffset()),
								within(1, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should retrieve invitation without a sender")
	void shouldRetrieveInvitationWithoutSender() {
		final var namespace = namespaces.findBySlug("konfigyr");
		assertThat(namespace).isPresent();

		assertThat(invitations.get(namespace.get(), "B1LPctaRXp6sxRo7"))
				.isPresent()
				.get()
				.returns("B1LPctaRXp6sxRo7", Invitation::key)
				.returns(namespace.get().id(), Invitation::namespace)
				.satisfies(it -> assertThat(it.recipient())
						.returns("expiring@konfigyr.com", Invitation.Recipient::email)
						.returns(false, Invitation.Recipient::exists)
						.returns(null, Invitation.Recipient::id)
						.returns(null, Invitation.Recipient::name)
				)
				.returns(NamespaceRole.USER, Invitation::role)
				.returns(true, Invitation::isExpired)
				.returns(null, Invitation::sender)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.expiryDate())
						.isCloseTo(OffsetDateTime.now().minusDays(10), within(1, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should fail to lookup invitation assigned to a different namespace")
	void shouldNotRetrieveInvitationForDifferentNamespace() {
		final var namespace = namespaces.findBySlug("john-doe");
		assertThat(namespace).isPresent();

		assertThat(invitations.get(namespace.get(), "qT6uq2ZP1Yv2bWmt"))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to lookup invitation that does not exist for a namespace")
	void shouldFailToRetrieveUnknownInvitation() {
		final var namespace = namespaces.findBySlug("konfigyr");
		assertThat(namespace).isPresent();

		assertThat(invitations.get(namespace.get(), "unknown-key"))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should accept pending invitation and create namespace member")
	void shouldAcceptInvitation(AssertablePublishedEvents events) {
		final var namespace = namespaces.findBySlug("konfigyr");
		assertThat(namespace).isPresent();

		final var invitation = invitations.get(namespace.get(), "qT6uq2ZP1Yv2bWmt");
		assertThat(invitation).isPresent();

		final var recipient = createAccount("invitee@konfigyr.com", "Paul", "Atreides");
		assertThat(recipient).isNotNull();

		assertThatNoException().isThrownBy(() -> invitations.accept(invitation.get(), recipient));

		assertThat(namespaces.findMembers(namespace.get()))
			.extracting(Member::account, Member::role)
			.contains(tuple(recipient, invitation.get().role()));

		assertThat(invitations.get(namespace.get(), "qT6uq2ZP1Yv2bWmt"))
				.isEmpty();

		events.assertThat()
				.contains(InvitationEvent.Accepted.class)
				.matching(event -> invitation.get().namespace().equals(event.id()))
				.matching(event -> invitation.get().namespace().equals(event.namespace()))
				.matching(event -> invitation.get().key().equals(event.key()));
	}

	@Test
	@Transactional
	@DisplayName("should fail to accept expiring invitations")
	void shouldNotAcceptExpiredInvitation() {
		final var namespace = namespaces.findBySlug("konfigyr");
		assertThat(namespace).isPresent();

		final var invitation = invitations.get(namespace.get(), "B1LPctaRXp6sxRo7");
		assertThat(invitation).isPresent();

		final var recipient = createAccount("expiring@konfigyr.com", "Leto", "Atreides");
		assertThat(recipient).isNotNull();

		assertThatThrownBy(() -> invitations.accept(invitation.get(), recipient))
				.isInstanceOf(InvitationException.class)
				.extracting("code")
				.isEqualTo(InvitationException.ErrorCode.INVITATION_EXPIRED);

		assertThat(namespaces.findMembers(namespace.get()))
				.extracting(Member::account)
				.doesNotContain(recipient);

		assertThat(invitations.get(namespace.get(), "qT6uq2ZP1Yv2bWmt"))
				.isPresent();
	}

	@Test
	@DisplayName("should fail to accept invitations with unknown recipient account")
	void shouldNotAcceptInvitationByUnknownAccount() {
		final var namespace = namespaces.findBySlug("konfigyr");
		assertThat(namespace).isPresent();

		final var invitation = invitations.get(namespace.get(), "qT6uq2ZP1Yv2bWmt");
		assertThat(invitation).isPresent();

		assertThatThrownBy(() -> invitations.accept(invitation.get(), EntityId.from(9999)))
				.isInstanceOf(InvitationException.class)
				.extracting("code")
				.isEqualTo(InvitationException.ErrorCode.RECIPIENT_NOT_FOUND);

		assertThat(namespaces.findMembers(namespace.get()))
				.extracting(Member::account)
				.doesNotContain(EntityId.from(9999));

		assertThat(invitations.get(namespace.get(), "qT6uq2ZP1Yv2bWmt"))
				.isPresent();
	}

	@Test
	@Transactional
	@DisplayName("should perform cleanup of expired invitations")
	void shouldCleanupExpiredInvitations() {
		assertThatNoException().isThrownBy(() -> ((DefaultInvitations) invitations).cleanup());

		final var namespace = namespaces.findBySlug("konfigyr");
		assertThat(namespace).isPresent();

		assertThat(invitations.find(namespace.get(), Pageable.unpaged()))
				.hasSize(1)
				.extracting(Invitation::key)
				.doesNotContain("B1LPctaRXp6sxRo7");

		assertThat(invitations.get(namespace.get(), "B1LPctaRXp6sxRo7"))
				.isEmpty();
	}

	private EntityId createAccount(String email, String firstName, String lastName) {
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
				.orElseThrow(() -> new IllegalStateException("Failed to create new test account"));
	}

}
