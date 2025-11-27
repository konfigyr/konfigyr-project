package com.konfigyr.account;

import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Result;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;
import static com.konfigyr.data.tables.NamespaceMembers.NAMESPACE_MEMBERS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

/**
 * Mapper class used to create records from {@link Record JOOQ records}.
 *
 * @author Vladimir Spasic
 **/
@RequiredArgsConstructor
final class AccountMapper {

	private final Name membershipsAlias;

	@NonNull
	@SuppressWarnings("unchecked")
	Account account(@NonNull Record record) {
		final var builder = Account.builder()
				.id(record.get(ACCOUNTS.ID))
				.status(record.get(ACCOUNTS.STATUS))
				.email(record.get(ACCOUNTS.EMAIL))
				.firstName(record.get(ACCOUNTS.FIRST_NAME))
				.lastName(record.get(ACCOUNTS.LAST_NAME))
				.avatar(record.get(ACCOUNTS.AVATAR))
				.lastLoginAt(record.get(ACCOUNTS.LAST_LOGIN_AT))
				.createdAt(record.get(ACCOUNTS.CREATED_AT))
				.updatedAt(record.get(ACCOUNTS.UPDATED_AT));

		final Field<?> memberships = record.field(membershipsAlias);

		if (memberships != null) {
			builder.memberships(record.get(memberships, List.class));
		}

		return builder.build();
	}

	@NonNull
	List<Membership> memberships(@NonNull Result<?> results) {
		return results.map(result -> result.map(this::membership));
	}

	@NonNull
	Membership membership(@NonNull Record record) {
		return Membership.builder()
				.id(record.get(NAMESPACE_MEMBERS.ID))
				.role(record.get(NAMESPACE_MEMBERS.ROLE))
				.namespace(record.get(NAMESPACES.SLUG))
				.name(record.get(NAMESPACES.NAME))
				.avatar(record.get(NAMESPACES.AVATAR))
				.since(record.get(NAMESPACE_MEMBERS.SINCE))
				.build();
	}

}
