package com.konfigyr.test;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountStatus;
import com.konfigyr.account.Membership;
import com.konfigyr.account.Memberships;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceRole;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Utility class that contains test {@link Account} stubs.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public interface TestAccounts {

	/**
	 * Creates a {@link Account.Builder} for <code>john.doe@konfigyr.com</code> test account.
	 *
	 * @return John Doe account builder, never {@literal null}
	 */
	static @NonNull Account.Builder john() {
		return Account.builder().id(1L)
				.status(AccountStatus.ACTIVE)
				.email("john.doe@konfigyr.com")
				.firstName("John")
				.lastName("Doe")
				.lastLoginAt(Instant.now().minus(5, ChronoUnit.MINUTES))
				.memberships(Memberships.of(
						Membership.builder()
								.id(EntityId.from(1L))
								.namespace("john-doe")
								.name("John Doe")
								.role(NamespaceRole.ADMIN)
								.since(Instant.now())
								.build(),
						Membership.builder()
								.id(EntityId.from(2L))
								.namespace("konfigyr")
								.name("Konfigyr")
								.role(NamespaceRole.ADMIN)
								.since(Instant.now())
								.build()
				));
	}

	/**
	 * Creates a {@link Account.Builder} for <code>jane.doe@konfigyr.com</code> test account.
	 *
	 * @return Jane Doe account builder, never {@literal null}
	 */
	static @NonNull Account.Builder jane() {
		return Account.builder().id(2L)
				.status(AccountStatus.ACTIVE)
				.email("jane.doe@konfigyr.com")
				.firstName("Jane")
				.lastName("Doe")
				.memberships(Memberships.of(
						Membership.builder()
								.id(EntityId.from(3L))
								.namespace("konfigyr")
								.name("Konfigyr")
								.role(NamespaceRole.USER)
								.since(Instant.now())
								.build()
				));
	}

}
