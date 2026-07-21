package com.konfigyr.membership;

import com.konfigyr.data.PageableExecutor;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceEvent;
import com.konfigyr.namespace.NamespaceException;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.support.FullName;
import com.konfigyr.support.SearchQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;
import static com.konfigyr.data.tables.NamespaceMembers.NAMESPACE_MEMBERS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

/**
 * Implementation of the {@link Memberships} that uses {@link DSLContext jOOQ} to communicate with
 * the persistence layer.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
class DefaultMemberships implements Memberships {

	static final PageableExecutor membersExecutor = PageableExecutor.builder()
			.defaultSortField(NAMESPACE_MEMBERS.SINCE.desc())
			.sortField("name", ACCOUNTS.FIRST_NAME)
			.sortField("date", NAMESPACE_MEMBERS.SINCE)
			.build();

	private final DSLContext context;
	private final ApplicationEventPublisher publisher;

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "namespace-find-members")
	public Page<@NonNull Member> find(@NonNull Namespace namespace, @NonNull SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();
		conditions.add(NAMESPACES.ID.eq(namespace.id().get()));

		query.term().map(term -> "%" + term + "%").ifPresent(term -> conditions.add(DSL.or(
				ACCOUNTS.EMAIL.likeIgnoreCase(term),
				ACCOUNTS.FIRST_NAME.likeIgnoreCase(term),
				ACCOUNTS.LAST_NAME.likeIgnoreCase(term)
		)));

		if (log.isDebugEnabled()) {
			log.debug("Fetching namespace members for namespace for conditions: {}", conditions);
		}

		return membersExecutor.execute(
				this::createMembersQuery,
				() -> DSL.and(conditions),
				DefaultMemberships::toMember,
				query.pageable()
		);
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "namespace-get-member")
	public Optional<Member> get(@NonNull Namespace namespace, @NonNull EntityId id) {
		return createMembersQuery().where(DSL.and(
				NAMESPACE_MEMBERS.ID.eq(id.get()),
				NAMESPACE_MEMBERS.NAMESPACE_ID.eq(namespace.id().get())
		)).fetchOptional(DefaultMemberships::toMember);
	}

	@NonNull
	@Override
	@Transactional(label = "namespace-update-member")
	public Member update(@NonNull Namespace namespace, @NonNull EntityId id, @NonNull NamespaceRole role) {
		if (isLastRemainingAdministrator(id) && NamespaceRole.USER == role) {
			throw new UnsupportedMembershipOperationException("Last administrator member cannot be updated to user");
		}

		context.update(NAMESPACE_MEMBERS)
				.set(NAMESPACE_MEMBERS.ROLE, role.name())
				.where(DSL.and(
						NAMESPACE_MEMBERS.ID.eq(id.get()),
						NAMESPACE_MEMBERS.NAMESPACE_ID.eq(namespace.id().get())
				))
				.execute();

		final Member member = createMembersQuery()
				.where(NAMESPACE_MEMBERS.ID.eq(id.get()))
				.fetchOptional(DefaultMemberships::toMember)
				.orElseThrow(() -> new NamespaceException("Failed to update unknown member with: " + id));

		log.info("Successfully updated member role: [id={}, namespace={}, account={}, role={}]",
				member.id(), namespace.slug(), member.account(), member.role());

		publisher.publishEvent(new NamespaceEvent.MemberUpdated(namespace, member.account(), member.role()));

		return member;
	}

	@Override
	@Transactional(label = "namespace-remove-member")
	public void remove(@NonNull Namespace namespace, @NonNull EntityId id) {
		if (isLastRemainingAdministrator(id)) {
			throw new UnsupportedMembershipOperationException("Last administrator member cannot be removed");
		}

		final Record result = context.deleteFrom(NAMESPACE_MEMBERS)
				.where(DSL.and(
						NAMESPACE_MEMBERS.ID.eq(id.get()),
						NAMESPACE_MEMBERS.NAMESPACE_ID.eq(namespace.id().get())
				))
				.returning(NAMESPACE_MEMBERS.ACCOUNT_ID)
				.fetchAny();

		if (result != null) {
			log.info("Successfully removed member: [id={}, namespace={}, account={}]",
					id.get(), namespace.slug(), result.get(NAMESPACE_MEMBERS.ACCOUNT_ID));

			publisher.publishEvent(new NamespaceEvent.MemberRemoved(
					namespace,
					result.get(NAMESPACE_MEMBERS.ACCOUNT_ID, EntityId.class)
			));
		}
	}

	boolean isLastRemainingAdministrator(@NonNull EntityId member) {
		final boolean exists = context.fetchExists(NAMESPACE_MEMBERS, NAMESPACE_MEMBERS.ID.eq(member.get()));

		if (!exists) {
			throw new MemberNotFoundException(member);
		}

		final List<Long> administrators = context.select(NAMESPACE_MEMBERS.ID)
				.from(NAMESPACE_MEMBERS)
				.where(DSL.and(
						NAMESPACE_MEMBERS.ROLE.eq(NamespaceRole.ADMIN.name()),
						NAMESPACE_MEMBERS.ID.ne(member.get()),
						NAMESPACE_MEMBERS.NAMESPACE_ID.eq(
								DSL.select(NAMESPACE_MEMBERS.NAMESPACE_ID)
										.from(NAMESPACE_MEMBERS)
										.where(NAMESPACE_MEMBERS.ID.eq(member.get()))
						)
				))
				.fetch(NAMESPACE_MEMBERS.ID);

		return CollectionUtils.isEmpty(administrators);
	}

	@NonNull
	private SelectOnConditionStep<Record> createMembersQuery() {
		return context.select(List.of(
						NAMESPACE_MEMBERS.ID,
						NAMESPACE_MEMBERS.NAMESPACE_ID,
						NAMESPACE_MEMBERS.ACCOUNT_ID,
						NAMESPACE_MEMBERS.ROLE,
						NAMESPACE_MEMBERS.SINCE,
						ACCOUNTS.EMAIL,
						ACCOUNTS.FIRST_NAME,
						ACCOUNTS.LAST_NAME,
						ACCOUNTS.AVATAR
				))
				.from(NAMESPACE_MEMBERS)
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(NAMESPACE_MEMBERS.NAMESPACE_ID))
				.innerJoin(ACCOUNTS)
				.on(ACCOUNTS.ID.eq(NAMESPACE_MEMBERS.ACCOUNT_ID));
	}

	@NonNull
	static Member toMember(@NonNull Record record) {
		final FullName fullName = FullName.of(
				record.get(ACCOUNTS.FIRST_NAME),
				record.get(ACCOUNTS.LAST_NAME)
		);

		return Member.builder()
				.id(record.get(NAMESPACE_MEMBERS.ID))
				.namespace(record.get(NAMESPACE_MEMBERS.NAMESPACE_ID))
				.account(record.get(NAMESPACE_MEMBERS.ACCOUNT_ID))
				.role(record.get(NAMESPACE_MEMBERS.ROLE))
				.email(record.get(ACCOUNTS.EMAIL))
				.fullName(fullName)
				.avatar(record.get(ACCOUNTS.AVATAR))
				.since(record.get(NAMESPACE_MEMBERS.SINCE))
				.build();
	}

}
