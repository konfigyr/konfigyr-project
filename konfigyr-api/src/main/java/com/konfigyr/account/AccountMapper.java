package com.konfigyr.account;

import org.jooq.Record;
import org.jspecify.annotations.NonNull;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;

/**
 * Mapper class used to create {@link Account} instances from jOOQ {@link Record records}.
 *
 * @author Vladimir Spasic
 **/
final class AccountMapper {

	@NonNull
	Account account(@NonNull Record record) {
		return Account.builder()
				.id(record.get(ACCOUNTS.ID))
				.status(record.get(ACCOUNTS.STATUS))
				.email(record.get(ACCOUNTS.EMAIL))
				.firstName(record.get(ACCOUNTS.FIRST_NAME))
				.lastName(record.get(ACCOUNTS.LAST_NAME))
				.avatar(record.get(ACCOUNTS.AVATAR))
				.lastLoginAt(record.get(ACCOUNTS.LAST_LOGIN_AT))
				.createdAt(record.get(ACCOUNTS.CREATED_AT))
				.updatedAt(record.get(ACCOUNTS.UPDATED_AT))
				.build();
	}

}
