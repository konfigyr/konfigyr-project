package com.konfigyr.membership;

import com.konfigyr.Hostnames;
import com.konfigyr.mail.Mail;
import com.konfigyr.mail.Mailer;
import com.konfigyr.support.FullName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;
import static com.konfigyr.data.tables.Invitations.INVITATIONS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;
import static org.springframework.util.ClassUtils.getShortName;

/**
 * Spring component that sends invitation emails when an {@link InvitationEvent.Created} event is published.
 * <p>
 * This listener runs asynchronously and is retried on transient failures. It is not retried for
 * {@link MembershipException} since those indicate a permanent error condition (e.g. the invitation
 * no longer exists).
 *
 * @author Vladimir Spasic
 * @see InvitationEvent.Created
 **/
@Slf4j
@RequiredArgsConstructor
class InvitationSender {

	private static final Marker INVITATION_SENT = MarkerFactory.getMarker("INVITATION_SENT");

	private final Mailer mailer;
	private final DSLContext context;
	private final Hostnames hostnames;

	@Async
	@Retryable(noRetryFor = MembershipException.class)
	@TransactionalEventListener(id = "invitation-sender", classes = InvitationEvent.Created.class)
	void send(InvitationEvent.Created event) {
		log.debug("Attempting to send out invitation email for event: {}", event);

		final Record invitation = lookupInvitation(event);
		final String namespace = invitation.get(NAMESPACES.SLUG);

		final String invitationLink = UriComponentsBuilder.fromUri(hostnames.web())
				.pathSegment("join", event.key())
				.toUriString();

		final Mail mail = Mail.builder()
				.subject("mail.invitation.subject", invitation.get(NAMESPACES.NAME))
				.template("mail/invitation")
				.to(invitation.get(INVITATIONS.RECIPIENT_EMAIL))
				.attribute("invitation", invitation)
				.attribute("namespace", invitation.get(NAMESPACES.NAME))
				.attribute("sender", FullName.of(invitation.get(ACCOUNTS.FIRST_NAME), invitation.get(ACCOUNTS.LAST_NAME)))
				.attribute("expiration", invitation.get(INVITATIONS.EXPIRY_DATE))
				.attribute("link", invitationLink)
				.build();

		mailer.send(mail);

		log.info(INVITATION_SENT, "Successfully sent an invitation email for: [namespace={}, key={}]",
				namespace, event.key());
	}

	private Record lookupInvitation(@NonNull InvitationEvent event) {
		if (log.isDebugEnabled()) {
			log.debug("Looking up invitation for event [type={}, namespace={}, key={}]",
					getShortName(event.getClass()), event.namespace(), event.key());
		}

		return context.select(
						NAMESPACES.SLUG,
						NAMESPACES.NAME,
						ACCOUNTS.FIRST_NAME,
						ACCOUNTS.LAST_NAME,
						INVITATIONS.RECIPIENT_EMAIL,
						INVITATIONS.EXPIRY_DATE
				)
				.from(INVITATIONS)
				.innerJoin(NAMESPACES)
				.on(INVITATIONS.NAMESPACE_ID.eq(NAMESPACES.ID))
				.innerJoin(ACCOUNTS)
				.on(INVITATIONS.SENDER_ID.eq(ACCOUNTS.ID))
				.where(DSL.and(
						INVITATIONS.KEY.eq(event.key()),
						INVITATIONS.NAMESPACE_ID.eq(event.namespace().get())
				))
				.fetchOptional()
				.orElseThrow(() -> new InvitationException(
						InvitationException.ErrorCode.INVITATION_NOT_FOUND,
						"Could not find invitation for key '" + event.key() + "' in namespace: " + event.namespace()
				));
	}

}
