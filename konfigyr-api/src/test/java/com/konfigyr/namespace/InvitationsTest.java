package com.konfigyr.namespace;

import com.konfigyr.account.AccountStatus;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.feature.FeatureValue;
import com.konfigyr.mail.Mail;
import com.konfigyr.support.FullName;
import com.konfigyr.test.AbstractIntegrationTest;
import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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
		final var namespace = lookupNamespace("konfigyr");

		final var invite = new Invite(
				EntityId.from(1),
				"recipient@konfigyr.com",
				NamespaceRole.USER
		);

		final var invitation = invitations.create(namespace, invite);

		assertThat(invitation)
				.isNotNull()
				.returns(invite.role(), Invitation::role)
				.satisfies(it -> assertThat(it.key())
						.isNotBlank()
						.hasSize(32)
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
		final var namespace = lookupNamespace("konfigyr");
		final var account = createAccount("peter.vries@arakis.com", "Piter", "De Vries");

		final var invite = new Invite(
				EntityId.from(1),
				"peter.vries@arakis.com",
				NamespaceRole.USER
		);

		final var invitation = invitations.create(namespace, invite);

		assertThat(invitation)
				.isNotNull()
				.returns(invite.role(), Invitation::role)
				.satisfies(it -> assertThat(it.key())
						.isNotBlank()
						.hasSize(32)
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
		final var namespace = lookupNamespace("konfigyr");
		final var invite = new Invite(
				EntityId.from(2),
				"recipient@konfigyr.com",
				NamespaceRole.USER
		);

		assertThatExceptionOfType(InvitationException.class)
				.isThrownBy(() -> invitations.create(namespace, invite))
				.extracting(InvitationException::getCode)
				.isEqualTo(InvitationException.ErrorCode.INSUFFICIENT_PERMISSIONS);

		Assertions.assertThat(events.ofType(Mail.class))
				.isEmpty();

		verifyNoInteractions(features);
	}

	@Test
	@DisplayName("should fail to create invitation for a recipient that is already a member")
	void shouldFailToCreateInvitationForExistingMember(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("konfigyr");
		final var invite = new Invite(
				EntityId.from(1),
				"john.doe@konfigyr.com",
				NamespaceRole.ADMIN
		);

		assertThatExceptionOfType(InvitationException.class)
				.isThrownBy(() -> invitations.create(namespace, invite))
				.extracting(InvitationException::getCode)
				.isEqualTo(InvitationException.ErrorCode.ALREADY_INVITED);

		Assertions.assertThat(events.ofType(Mail.class))
				.isEmpty();

		verifyNoInteractions(features);
	}

	@Test
	@DisplayName("should fail to create invitation when no feature has been assigned to a Namespace")
	void shouldFailToCreateInvitationForMissingFeatureValue(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("john-doe");
		doReturn(Optional.empty()).when(features).get(namespace.slug(), NamespaceFeatures.MEMBERS_COUNT);

		final var invite = new Invite(
				EntityId.from(1),
				"recipient@konfigyr.com",
				NamespaceRole.USER
		);

		assertThatExceptionOfType(InvitationException.class)
				.isThrownBy(() -> invitations.create(namespace, invite))
				.extracting(InvitationException::getCode)
				.isEqualTo(InvitationException.ErrorCode.NOT_ALLOWED);

		Assertions.assertThat(events.ofType(Mail.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to create invitation when Namespace has reached maximum member count")
	void shouldFailToCreateInvitationWhenMaximumCountIsReached(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("konfigyr");
		doReturn(Optional.of(FeatureValue.limited(2))).when(features).get(namespace.slug(), NamespaceFeatures.MEMBERS_COUNT);

		final var invite = new Invite(
				EntityId.from(1),
				"recipient@konfigyr.com",
				NamespaceRole.USER
		);

		assertThatExceptionOfType(InvitationException.class)
				.isThrownBy(() -> invitations.create(namespace, invite))
				.extracting(InvitationException::getCode)
				.isEqualTo(InvitationException.ErrorCode.MEMBER_LIMIT_REACHED);

		Assertions.assertThat(events.ofType(Mail.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve all invitations for namespace")
	void shouldRetrieveInvitations() {
		final var namespace = lookupNamespace("konfigyr");

		assertThat(invitations.find(namespace, Pageable.unpaged()))
				.hasSize(2)
				.extracting(Invitation::key)
				.containsExactlyInAnyOrder("09320d7f8e21143b2957f1caded74cbc", "09320f6c6481c1fed73573a5430758f1");
	}

	@Test
	@DisplayName("should retrieve no invitations for namespace that has no invitations")
	void shouldRetrieveEmptyInvitations() {
		final var namespace = lookupNamespace("john-doe");

		assertThat(invitations.find(namespace, Pageable.unpaged()))
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve invitation by namespace and key")
	void shouldRetrieveInvitation() {
		final var namespace = lookupNamespace("konfigyr");

		assertThat(invitations.get(namespace, "09320d7f8e21143b2957f1caded74cbc"))
				.isPresent()
				.get()
				.returns("09320d7f8e21143b2957f1caded74cbc", Invitation::key)
				.returns(namespace.id(), Invitation::namespace)
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
		final var namespace = lookupNamespace("konfigyr");

		assertThat(invitations.get(namespace, "09320f6c6481c1fed73573a5430758f1"))
				.isPresent()
				.get()
				.returns("09320f6c6481c1fed73573a5430758f1", Invitation::key)
				.returns(namespace.id(), Invitation::namespace)
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
		final var namespace = lookupNamespace("john-doe");

		assertThat(invitations.get(namespace, "09320d7f8e21143b2957f1caded74cbc"))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to lookup invitation that does not exist for a namespace")
	void shouldFailToRetrieveUnknownInvitation() {
		final var namespace = lookupNamespace("konfigyr");

		assertThat(invitations.get(namespace, "unknown-key"))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should accept pending invitation and create namespace member")
	void shouldAcceptInvitation(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("konfigyr");

		final var invitation = invitations.get(namespace, "09320d7f8e21143b2957f1caded74cbc");
		assertThat(invitation).isPresent();

		final var recipient = createAccount("invitee@konfigyr.com", "Paul", "Atreides");
		assertThat(recipient).isNotNull();

		assertThatNoException().isThrownBy(() -> invitations.accept(namespace, invitation.get(), recipient));

		assertThat(namespaces.findMembers(namespace))
			.extracting(Member::account, Member::role)
			.contains(tuple(recipient, invitation.get().role()));

		assertThat(invitations.get(namespace, "09320d7f8e21143b2957f1caded74cbc"))
				.isEmpty();

		events.assertThat()
				.contains(InvitationEvent.Accepted.class)
				.matching(event -> invitation.get().namespace().equals(event.id()))
				.matching(event -> invitation.get().namespace().equals(event.namespace()))
				.matching(event -> invitation.get().key().equals(event.key()));

		events.assertThat()
				.contains(NamespaceEvent.MemberAdded.class)
				.matching(event -> invitation.get().namespace().equals(event.id()))
				.matching(event -> recipient.equals(event.account()))
				.matching(event -> invitation.get().role().equals(event.role()));
	}

	@Test
	@Transactional
	@DisplayName("should fail to accept expiring invitations")
	void shouldNotAcceptExpiredInvitation() {
		final var namespace = lookupNamespace("konfigyr");

		final var invitation = invitations.get(namespace, "09320f6c6481c1fed73573a5430758f1");
		assertThat(invitation).isPresent();

		final var recipient = createAccount("expiring@konfigyr.com", "Leto", "Atreides");
		assertThat(recipient).isNotNull();

		assertThatExceptionOfType(InvitationException.class)
				.isThrownBy(() -> invitations.accept(namespace, invitation.get(), recipient))
				.extracting(InvitationException::getCode)
				.isEqualTo(InvitationException.ErrorCode.INVITATION_EXPIRED);

		assertThat(namespaces.findMembers(namespace))
				.extracting(Member::account)
				.doesNotContain(recipient);

		assertThat(invitations.get(namespace, "09320d7f8e21143b2957f1caded74cbc"))
				.isPresent();
	}

	@Test
	@DisplayName("should fail to accept invitations with unknown recipient account")
	void shouldNotAcceptInvitationByUnknownAccount() {
		final var namespace = lookupNamespace("konfigyr");

		final var invitation = invitations.get(namespace, "09320d7f8e21143b2957f1caded74cbc");
		assertThat(invitation).isPresent();

		assertThatExceptionOfType(InvitationException.class)
				.isThrownBy(() -> invitations.accept(namespace, invitation.get(), EntityId.from(9999)))
				.extracting(InvitationException::getCode)
				.isEqualTo(InvitationException.ErrorCode.RECIPIENT_NOT_FOUND);

		assertThat(namespaces.findMembers(namespace))
				.extracting(Member::account)
				.doesNotContain(EntityId.from(9999));

		assertThat(invitations.get(namespace, "09320d7f8e21143b2957f1caded74cbc"))
				.isPresent();
	}

	@Test
	@Transactional
	@DisplayName("should perform cleanup of expired invitations")
	void shouldCleanupExpiredInvitations() {
		assertThatNoException().isThrownBy(() -> ((DefaultInvitations) invitations).cleanup());

		final var namespace = lookupNamespace("konfigyr");

		assertThat(invitations.find(namespace, Pageable.unpaged()))
				.hasSize(1)
				.extracting(Invitation::key)
				.doesNotContain("09320f6c6481c1fed73573a5430758f1");

		assertThat(invitations.get(namespace, "09320f6c6481c1fed73573a5430758f1"))
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

	private Namespace lookupNamespace(String slug) {
		return assertThat(namespaces.findBySlug(slug))
				.as("Namespace with slug '%s' not found", slug)
				.isPresent()
				.get()
				.actual();
	}

}
