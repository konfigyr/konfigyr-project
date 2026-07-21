package com.konfigyr.membership;

import com.konfigyr.account.Account;
import com.konfigyr.crypto.TokenGenerator;
import com.konfigyr.data.Keys;
import com.konfigyr.data.PageableExecutor;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.data.tables.Accounts;
import com.konfigyr.entity.EntityId;
import com.konfigyr.feature.Features;
import com.konfigyr.feature.LimitedFeatureValue;
import com.konfigyr.namespace.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;
import static com.konfigyr.data.tables.Invitations.INVITATIONS;
import static com.konfigyr.data.tables.NamespaceMembers.NAMESPACE_MEMBERS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

/**
 * Default implementation of the {@link Invitations} service.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Slf4j
@NullMarked
@RequiredArgsConstructor
class DefaultInvitations implements Invitations {

	static final Accounts SENDER_ACCOUNTS = ACCOUNTS.as("sender");
	static final Accounts RECIPIENT_ACCOUNTS = ACCOUNTS.as("recipient");

	static final Duration TTL = Duration.ofDays(7);

	static final PageableExecutor executor = PageableExecutor.builder()
			.defaultSortField(INVITATIONS.CREATED_AT.desc())
			.sortField("date", INVITATIONS.CREATED_AT)
			.sortField("recipient", INVITATIONS.RECIPIENT_EMAIL)
			.build();

	private final StringKeyGenerator keyGenerator = TokenGenerator.getInstance();

	private final DSLContext context;
	private final Features features;
	private final NamespaceManager namespaces;
	private final ApplicationEventPublisher publisher;

	@Override
	@Transactional(readOnly = true, label = "invitations-find-for-recipient")
	public Page<Invitation> find(Account recipient, Pageable pageable) {
		return executor.execute(
				this::createInvitationsQuery,
				() -> INVITATIONS.RECIPIENT_EMAIL.equalIgnoreCase(recipient.email()),
				DefaultInvitations::invitation,
				pageable
		);
	}

	@Override
	@Transactional(readOnly = true, label = "invitations-find-for-namespace")
	public Page<Invitation> find(Namespace namespace, Pageable pageable) {
		return executor.execute(
				this::createInvitationsQuery,
				() -> INVITATIONS.NAMESPACE_ID.eq(namespace.id().get()),
				DefaultInvitations::invitation,
				pageable
		);
	}

	@Override
	@Transactional(readOnly = true, label = "invitations-retrieve-for-account")
	public Optional<Invitation> get(Account recipient, String key) {
		return lookupInvitation(DSL.and(
				INVITATIONS.RECIPIENT_EMAIL.equalIgnoreCase(recipient.email()),
				INVITATIONS.KEY.eq(key)
		));
	}

	@Override
	@Transactional(readOnly = true, label = "invitations-retrieve-for-namespace")
	public Optional<Invitation> get(Namespace namespace, String key) {
		return lookupInvitation(DSL.and(
				INVITATIONS.NAMESPACE_ID.eq(namespace.id().get()),
				INVITATIONS.KEY.eq(key)
		));
	}

	@Override
	@Transactional(label = "invitations-create")
	public Invitation create(Namespace namespace, Invite invite) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to create and send new invitation for: [namespace={}, sender={}, role={}",
					namespace.slug(), invite.sender(), invite.role());
		}

		final NamespaceInvitationContext context = lookupNamespaceInvitationContext(namespace, invite).orElseThrow(
				() -> new InvitationException(InvitationException.ErrorCode.INSUFFICIENT_PERMISSIONS,
						"Not enough permissions to create invitation")
		);

		if (context.isMember()) {
			throw new InvitationException(InvitationException.ErrorCode.ALREADY_INVITED,
					"Can not create invitation as the recipient is already a namespace member");
		}

		assertNamespaceMemberCount(context);

		final String key = this.context.insertInto(INVITATIONS)
				.set(
						SettableRecord.of(INVITATIONS)
								.set(INVITATIONS.KEY, keyGenerator.generateKey())
								.set(INVITATIONS.NAMESPACE_ID, namespace.id().get())
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
				INVITATIONS.NAMESPACE_ID.eq(namespace.id().get())
		)).orElseThrow(
				() -> new IllegalStateException("Failed to create invitation for: " + invite)
		);

		log.info("Invitation has been created for namespace '{}' with: [key={}, role={}, sender={}]",
				context.name(), key, invite.role(), invite.sender());

		publisher.publishEvent(new InvitationEvent.Created(namespace, invitation.key()));

		return invitation;
	}

	@Override
	@Transactional(label = "invitations-accept")
	public void accept(Account recipient, Invitation invitation) {
		if (!invitation.recipient().email().equalsIgnoreCase(recipient.email())) {
			throw new InvitationException(InvitationException.ErrorCode.INVITATION_NOT_FOUND,
					"The recipient of the invitation does not match the account accepting the invitation");
		}

		if (invitation.isExpired()) {
			throw new InvitationException(InvitationException.ErrorCode.INVITATION_EXPIRED,
					"Can not accept expiring invitations");
		}

		try {
			context.insertInto(NAMESPACE_MEMBERS)
					.set(
							SettableRecord.of(NAMESPACE_MEMBERS)
									.set(NAMESPACE_MEMBERS.ID, EntityId.generate().map(EntityId::get))
									.set(NAMESPACE_MEMBERS.ACCOUNT_ID, recipient.id().get())
									.set(NAMESPACE_MEMBERS.NAMESPACE_ID, invitation.organization().id().get())
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
					"Could not find recipient account with identifier: " + recipient, ex);
		}

		removeInvitation(invitation);

		final Namespace namespace = lookupNamespaceForInvitation(invitation);

		publisher.publishEvent(new InvitationEvent.Accepted(namespace, recipient, invitation.key()));
		publisher.publishEvent(new NamespaceEvent.MemberAdded(namespace, recipient.id(), invitation.role()));
	}

	@Override
	@Transactional(label = "invitations-decline")
	public void decline(Account recipient, Invitation invitation) {
		removeInvitation(invitation);

		publisher.publishEvent(new InvitationEvent.Declined(
				lookupNamespaceForInvitation(invitation), recipient, invitation.key())
		);
	}

	@Override
	@Transactional(label = "invitations-cancel")
	public void cancel(Namespace namespace, Invitation invitation) {
		removeInvitation(invitation);

		publisher.publishEvent(new InvitationEvent.Canceled(namespace, invitation.key()));
	}

	/**
	 * Cleanup job that runs every hour and deletes expired {@link Invitation invitations} from
	 * the database.
	 * <p>
	 * To change the execution interval, set the {@code konfigyr.invitations.cleanup.cron}
	 * configuration property to a valid cron expression. To disable this task, set it to {@code '-'}.
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

	private Optional<Invitation> lookupInvitation(Condition condition) {
		return createInvitationsQuery()
				.where(condition)
				.fetchOptional(DefaultInvitations::invitation);
	}

	private void removeInvitation(Invitation invitation) {
		context.deleteFrom(INVITATIONS)
				.where(DSL.and(
						INVITATIONS.NAMESPACE_ID.eq(invitation.organization().id().get()),
						INVITATIONS.KEY.eq(invitation.key())
				))
				.execute();
	}

	private SelectOnConditionStep<Record> createInvitationsQuery() {
		return context
				.select(List.of(
						NAMESPACES.ID,
						NAMESPACES.SLUG,
						NAMESPACES.NAME,
						NAMESPACES.DESCRIPTION,
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
						INVITATIONS.EXPIRY_DATE))
				.from(INVITATIONS)
				.fullOuterJoin(SENDER_ACCOUNTS)
				.on(SENDER_ACCOUNTS.ID.eq(INVITATIONS.SENDER_ID))
				.fullOuterJoin(RECIPIENT_ACCOUNTS)
				.on(RECIPIENT_ACCOUNTS.ID.eq(INVITATIONS.RECIPIENT_ID))
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(INVITATIONS.NAMESPACE_ID));
	}

	private Optional<NamespaceInvitationContext> lookupNamespaceInvitationContext(Namespace namespace, Invite invite) {
		return context.select(
						NAMESPACES.ID,
						NAMESPACES.SLUG,
						NAMESPACES.NAME,
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
				.innerJoin(SENDER_ACCOUNTS)
				.on(NAMESPACE_MEMBERS.ACCOUNT_ID.eq(SENDER_ACCOUNTS.ID))
				.fullOuterJoin(RECIPIENT_ACCOUNTS)
				.on(RECIPIENT_ACCOUNTS.EMAIL.equalIgnoreCase(invite.recipient()))
				.innerJoin(NAMESPACES)
				.on(NAMESPACE_MEMBERS.NAMESPACE_ID.eq(NAMESPACES.ID))
				.where(DSL.and(
						NAMESPACE_MEMBERS.ACCOUNT_ID.eq(invite.sender().get()),
						NAMESPACE_MEMBERS.NAMESPACE_ID.eq(namespace.id().get()),
						NAMESPACE_MEMBERS.ROLE.eq(NamespaceRole.ADMIN.name())
				))
				.fetchOptional(record -> NamespaceInvitationContext.create(record, invite));
	}

	private void assertNamespaceMemberCount(NamespaceInvitationContext context) {
		log.debug("Checking if Namespace({}) can invite additional members", context.slug());

		final LimitedFeatureValue feature = features.get(context.slug(), NamespaceFeatures.MEMBERS_COUNT)
				.orElseThrow(() -> new InvitationException(InvitationException.ErrorCode.NOT_ALLOWED,
						"Please upgrade your plan to invite team members"));

		if (feature.isUnlimited()) {
			return;
		}

		final int count = this.context.fetchCount(NAMESPACE_MEMBERS, NAMESPACE_MEMBERS.NAMESPACE_ID.eq(context.id().get()));

		if (count >= feature.get()) {
			throw new InvitationException(InvitationException.ErrorCode.MEMBER_LIMIT_REACHED,
					"Can not create invitation as maximum number of members has been reached");
		}
	}

	private Namespace lookupNamespaceForInvitation(Invitation invitation) {
		return namespaces.findById(invitation.organization().id()).orElseThrow(() -> new IllegalStateException(
				"Fail to find namespace by %s for invitation '%s'".formatted(invitation.organization().id(), invitation.key())
		));
	}

	private static Invitation.@Nullable Sender sender(Record record) {
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

	private static Invitation invitation(Record record) {
		final OffsetDateTime expiryDate = record.get(INVITATIONS.EXPIRY_DATE);
		final InvitationState state = OffsetDateTime.now().isAfter(expiryDate)
				? InvitationState.EXPIRED
				: InvitationState.PENDING;

		return Invitation.builder()
				.key(record.get(INVITATIONS.KEY))
				.organization(new Invitation.Organization(
						EntityId.from(record.get(NAMESPACES.ID)),
						record.get(NAMESPACES.SLUG),
						record.get(NAMESPACES.NAME),
						record.get(NAMESPACES.DESCRIPTION)
				))
				.sender(sender(record))
				.recipient(recipient(record, record.get(INVITATIONS.RECIPIENT_EMAIL)))
				.role(NamespaceRole.valueOf(record.get(INVITATIONS.ROLE)))
				.state(state)
				.createdAt(record.get(INVITATIONS.CREATED_AT))
				.expiryDate(expiryDate)
				.build();
	}

	private record NamespaceInvitationContext(
			EntityId id,
			String slug,
			String name,
			Invitation.Sender sender,
			Invitation.Recipient recipient,
			boolean isMember
	) {
		static NamespaceInvitationContext create(Record record, Invite invite) {
			return new NamespaceInvitationContext(
					record.get(NAMESPACES.ID, EntityId.class),
					record.get(NAMESPACES.SLUG),
					record.get(NAMESPACES.NAME),
					Objects.requireNonNull(DefaultInvitations.sender(record)),
					DefaultInvitations.recipient(record, invite.recipient()),
					Boolean.TRUE.equals(record.get(RECIPIENT_ACCOUNTS.ID.in(NAMESPACE_MEMBERS.ACCOUNT_ID)))
			);
		}
	}

}
