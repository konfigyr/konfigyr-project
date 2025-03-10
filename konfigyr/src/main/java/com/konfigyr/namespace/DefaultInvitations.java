package com.konfigyr.namespace;

import com.konfigyr.data.Keys;
import com.konfigyr.data.tables.Accounts;
import com.konfigyr.entity.EntityId;
import com.konfigyr.data.PageableExecutor;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.support.KeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

import static com.konfigyr.data.tables.Invitations.INVITATIONS;
import static com.konfigyr.data.tables.NamespaceMembers.NAMESPACE_MEMBERS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;
import static com.konfigyr.data.tables.Accounts.ACCOUNTS;

/**
 * Default implementation of the {@link Invitations} service.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
class DefaultInvitations implements Invitations {

	static Accounts SENDER_ACCOUNTS = ACCOUNTS.as("sender");
	static Accounts RECIPIENT_ACCOUNTS = ACCOUNTS.as("recipient");

	static final Duration TTL = Duration.ofDays(7);

	static final PageableExecutor executor = PageableExecutor.builder()
			.defaultSortField(INVITATIONS.CREATED_AT.desc())
			.sortField("date", INVITATIONS.CREATED_AT)
			.sortField("recipient", INVITATIONS.RECIPIENT_EMAIL)
			.build();

	private final DSLContext context;
	private final ApplicationEventPublisher publisher;

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "invitations-find")
	public Page<Invitation> find(@NonNull Namespace namespace, @NonNull Pageable pageable) {
		final Condition condition = INVITATIONS.NAMESPACE_ID.eq(namespace.id().get());

		return executor.execute(
				createInvitationsQuery(condition),
				DefaultInvitations::invitation,
				pageable,
				() -> context.fetchCount(createInvitationsQuery(condition))
		);
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "invitations-retrieve")
	public Optional<Invitation> get(@NonNull Namespace namespace, @NonNull String key) {
		if (log.isDebugEnabled()) {
			log.debug("Retrieving invitation with key {} for namespace: {}", key, namespace.slug());
		}

		return lookupInvitation(DSL.and(
				INVITATIONS.NAMESPACE_ID.eq(namespace.id().get()),
				INVITATIONS.KEY.eq(key)
		));
	}

	@NonNull
	@Override
	@Transactional(label = "invitations-create")
	public Invitation create(@NonNull Invite invite) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to create and send new invitation for: [namespace={}, sender={}, role={}",
					invite.namespace(), invite.sender(), invite.role());
		}

		final NamespaceInvitationContext context = lookupNamespaceInvitationContext(invite).orElseThrow(
				() -> new InvitationException(InvitationException.ErrorCode.INSUFFICIENT_PERMISSIONS,
						"Not enough permissions to create invitation")
		);

		if (NamespaceType.PERSONAL == context.type) {
			throw new InvitationException(InvitationException.ErrorCode.UNSUPPORTED_NAMESPACE_TYPE,
					"Can not create invitations for personal namespaces");
		}

		if (context.isMember()) {
			throw new InvitationException(InvitationException.ErrorCode.ALREADY_INVITED,
					"Can not create invitation as the recipient is already a namespace member");
		}

		final String key = this.context.insertInto(INVITATIONS)
				.set(
						SettableRecord.of(INVITATIONS)
								.set(INVITATIONS.KEY, KeyGenerator.getInstance().generateKey())
								.set(INVITATIONS.NAMESPACE_ID, invite.namespace().get())
								.set(INVITATIONS.SENDER_ID, context.sender().id(), EntityId::get)
								.set(INVITATIONS.RECIPIENT_ID, context.recipient().id(), EntityId::get)
								.set(INVITATIONS.RECIPIENT_EMAIL, context.recipient().email())
								.set(INVITATIONS.ROLE, invite.role().name())
								.set(INVITATIONS.EXPIRY_DATE, OffsetDateTime.now().plus(TTL))
								.get()
				)
				.onConflictOnConstraint(Keys.UNIQUE_NAMESPACE_INVITATION.constraint())
				.doUpdate()
				.set(INVITATIONS.SENDER_ID, invite.sender().get())
				.set(INVITATIONS.ROLE, invite.role().name())
				.set(INVITATIONS.EXPIRY_DATE, OffsetDateTime.now().plus(TTL))
				.returning(INVITATIONS.KEY)
				.fetchOne(INVITATIONS.KEY);

		Assert.state(StringUtils.hasText(key), "Failed to create invitation for: " + invite);

		final Invitation invitation = lookupInvitation(DSL.and(
				INVITATIONS.KEY.eq(key),
				INVITATIONS.NAMESPACE_ID.eq(invite.namespace().get())
		)).orElseThrow(
				() -> new IllegalStateException("Failed to create invitation for: " + invite)
		);

		log.info("Invitation has been created for namespace '{}' with: [key{}, role={}, sender={}]",
				context.name(), key, invite.role(), invite.sender());

		final UriComponents host = ServletUriComponentsBuilder.fromCurrentServletMapping().build();
		publisher.publishEvent(new InvitationEvent.Created(invitation.namespace(), invitation.key(), host));

		return invitation;
	}

	@Override
	@Transactional(label = "invitations-accept")
	public void accept(@NonNull Invitation invitation, @NonNull EntityId recipient) {
		if (invitation.isExpired()) {
			throw new InvitationException(InvitationException.ErrorCode.INVITATION_EXPIRED,
					"Can not accept expiring invitations");
		}

		try {
			context.insertInto(NAMESPACE_MEMBERS)
					.set(
							SettableRecord.of(NAMESPACE_MEMBERS)
									.set(NAMESPACE_MEMBERS.ID, EntityId.generate().map(EntityId::get))
									.set(NAMESPACE_MEMBERS.ACCOUNT_ID, recipient.get())
									.set(NAMESPACE_MEMBERS.NAMESPACE_ID, invitation.namespace().get())
									.set(NAMESPACE_MEMBERS.ROLE, invitation.role().name())
									.set(NAMESPACE_MEMBERS.SINCE, OffsetDateTime.now())
									.get()
					)
					.onConflictOnConstraint(Keys.UNIQUE_NAMESPACE_MEMBER)
					.doUpdate()
					.set(NAMESPACE_MEMBERS.ROLE, invitation.role().name())
					.execute();
		} catch (DataIntegrityViolationException ex) {
			throw new InvitationException(InvitationException.ErrorCode.RECIPIENT_NOT_FOUND,
					"Could not find recipient account with identifier: " + recipient);
		}

		context.deleteFrom(INVITATIONS)
				.where(DSL.and(
						INVITATIONS.NAMESPACE_ID.eq(invitation.namespace().get()),
						INVITATIONS.KEY.eq(invitation.key())
				))
				.execute();

		publisher.publishEvent(new InvitationEvent.Accepted(invitation.namespace(), invitation.key()));

		publisher.publishEvent(new NamespaceEvent.MemberAdded(
				invitation.namespace(), EntityId.from(recipient.get()), invitation.role()
		));
	}

	/**
	 * Cleanup job that is executed every hour that would delete expiring {@link Invitation invitations}
	 * from the database.
	 * <p>
	 * If you wish to change the job execution interval, use the <code>konfigyr.invitations.cleanup.cron</code>
	 * configuration property and specify your own cron expression.
	 * <p>
	 * To disable this task set the value of the crop configuration property to {@code '-'}.
	 *
	 * @see org.springframework.scheduling.support.CronExpression
	 * @see org.springframework.scheduling.config.ScheduledTaskRegistrar#CRON_DISABLED
	 */
	@Transactional(label = "invitations-cleanup")
	@Scheduled(cron = "${konfigyr.invitations.cleanup.cron:0 0 * * * *}")
	public void cleanup() {
		final long count = context.deleteFrom(INVITATIONS)
				.where(INVITATIONS.EXPIRY_DATE.lessThan(OffsetDateTime.now()))
				.execute();

		if (count > 0) {
			log.info("Successfully cleaned up {} expired invitations", count);
		}
	}

	@NonNull
	private Optional<Invitation> lookupInvitation(@NonNull Condition predicate) {
		return createInvitationsQuery(predicate).fetchOptional(DefaultInvitations::invitation);
	}

	@NonNull
	private SelectConditionStep<? extends Record> createInvitationsQuery(Condition condition) {
		return context
				.select(
						NAMESPACES.ID,
						SENDER_ACCOUNTS.ID,
						SENDER_ACCOUNTS.EMAIL,
						SENDER_ACCOUNTS.FIRST_NAME,
						SENDER_ACCOUNTS.LAST_NAME,
						RECIPIENT_ACCOUNTS.ID,
						RECIPIENT_ACCOUNTS.FIRST_NAME,
						RECIPIENT_ACCOUNTS.LAST_NAME,
						INVITATIONS.KEY,
						INVITATIONS.RECIPIENT_EMAIL,
						INVITATIONS.ROLE,
						INVITATIONS.CREATED_AT,
						INVITATIONS.EXPIRY_DATE)
				.from(INVITATIONS)
				.fullOuterJoin(SENDER_ACCOUNTS)
				.on(SENDER_ACCOUNTS.ID.eq(INVITATIONS.SENDER_ID))
				.fullOuterJoin(RECIPIENT_ACCOUNTS)
				.on(RECIPIENT_ACCOUNTS.ID.eq(INVITATIONS.RECIPIENT_ID))
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(INVITATIONS.NAMESPACE_ID))
				.where(condition);
	}

	@NonNull
	private Optional<NamespaceInvitationContext> lookupNamespaceInvitationContext(@NonNull Invite invite) {
		return context.select(
						NAMESPACES.SLUG,
						NAMESPACES.NAME,
						NAMESPACES.TYPE,
						SENDER_ACCOUNTS.ID,
						SENDER_ACCOUNTS.EMAIL,
						SENDER_ACCOUNTS.FIRST_NAME,
						SENDER_ACCOUNTS.LAST_NAME,
						RECIPIENT_ACCOUNTS.ID,
						RECIPIENT_ACCOUNTS.FIRST_NAME,
						RECIPIENT_ACCOUNTS.LAST_NAME,
						RECIPIENT_ACCOUNTS.ID.in(NAMESPACE_MEMBERS.ACCOUNT_ID)
				)
				.from(NAMESPACE_MEMBERS)
				/* join sender accounts to retrieve sender information */
				.innerJoin(SENDER_ACCOUNTS)
				.on(NAMESPACE_MEMBERS.ACCOUNT_ID.eq(SENDER_ACCOUNTS.ID))
				/* join recipient accounts to check if recipient is already known */
				.fullOuterJoin(RECIPIENT_ACCOUNTS)
				.on(RECIPIENT_ACCOUNTS.EMAIL.equalIgnoreCase(invite.recipient()))
				/* join namespaces to retrieve namespace information */
				.innerJoin(NAMESPACES)
				.on(NAMESPACE_MEMBERS.NAMESPACE_ID.eq(NAMESPACES.ID))
				.where(DSL.and(
						NAMESPACE_MEMBERS.ACCOUNT_ID.eq(invite.sender().get()),
						NAMESPACE_MEMBERS.NAMESPACE_ID.eq(invite.namespace().get()),
						NAMESPACE_MEMBERS.ROLE.eq(NamespaceRole.ADMIN.name())
				))
				.fetchOptional(record -> NamespaceInvitationContext.create(record, invite));
	}

	@Nullable
	private static Invitation.Sender sender(Record record) {
		if (record.get(SENDER_ACCOUNTS.ID) == null) {
			return null;
		}

		return new Invitation.Sender(
				EntityId.from(record.get(SENDER_ACCOUNTS.ID)),
				record.get(SENDER_ACCOUNTS.EMAIL),
				record.get(SENDER_ACCOUNTS.FIRST_NAME),
				record.get(SENDER_ACCOUNTS.LAST_NAME)
		);
	}

	@NonNull
	private static Invitation.Recipient recipient(Record record, String email) {
		if (record.get(RECIPIENT_ACCOUNTS.ID) == null) {
			return new Invitation.Recipient(email);
		}

		return new Invitation.Recipient(
				EntityId.from(record.get(RECIPIENT_ACCOUNTS.ID)),
				email,
				record.get(RECIPIENT_ACCOUNTS.FIRST_NAME),
				record.get(RECIPIENT_ACCOUNTS.LAST_NAME)
		);
	}

	@NonNull
	private static Invitation invitation(Record record) {
		return Invitation.builder()
				.key(record.get(INVITATIONS.KEY))
				.namespace(record.get(NAMESPACES.ID))
				.sender(sender(record))
				.recipient(recipient(record, record.get(INVITATIONS.RECIPIENT_EMAIL)))
				.role(NamespaceRole.valueOf(record.get(INVITATIONS.ROLE)))
				.createdAt(record.get(INVITATIONS.CREATED_AT))
				.expiryDate(record.get(INVITATIONS.EXPIRY_DATE))
				.build();
	}

	private record NamespaceInvitationContext(
			@NonNull String slug,
			@NonNull String name,
			@NonNull NamespaceType type,
			@NonNull Invitation.Sender sender,
			@NonNull Invitation.Recipient recipient,
			boolean isMember
	) {
		static NamespaceInvitationContext create(@NonNull Record record, @NonNull Invite invite) {
			return new NamespaceInvitationContext(
					record.get(NAMESPACES.SLUG),
					record.get(NAMESPACES.NAME),
					NamespaceType.valueOf(record.get(NAMESPACES.TYPE)),
					Objects.requireNonNull(DefaultInvitations.sender(record)),
					DefaultInvitations.recipient(record, invite.recipient()),
					Boolean.TRUE.equals(record.get(RECIPIENT_ACCOUNTS.ID.in(NAMESPACE_MEMBERS.ACCOUNT_ID)))
			);
		}
	}

}
