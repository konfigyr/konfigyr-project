package com.konfigyr.identity;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.AccountIdentityStatus;
import com.konfigyr.support.Avatar;
import org.springframework.security.core.authority.AuthorityUtils;

public interface AccountIdentities {

	/**
	 * Creates a {@link AccountIdentity.Builder} for <code>john.doe@konfigyr.com</code> test account.
	 *
	 * @return John Doe account identity builder, never {@literal null}
	 */
	static AccountIdentity.Builder john() {
		return AccountIdentity.builder()
				.id(EntityId.from(1))
				.status(AccountIdentityStatus.ACTIVE)
				.email("john.doe@konfigyr.com")
				.displayName("John Doe")
				.avatar(Avatar.generate(EntityId.from(1), "JD"))
				.authorities(AuthorityUtils.commaSeparatedStringToAuthorityList("john"));
	}

	/**
	 * Creates a {@link AccountIdentity.Builder} for <code>jane.doe@konfigyr.com</code> test account.
	 *
	 * @return Jane Doe account identity builder, never {@literal null}
	 */
	static AccountIdentity.Builder jane() {
		return AccountIdentity.builder()
				.id(EntityId.from(2))
				.status(AccountIdentityStatus.ACTIVE)
				.email("jane.doe@konfigyr.com")
				.displayName("Jane Doe")
				.avatar(Avatar.generate(EntityId.from(2), "JD"))
				.authorities(AuthorityUtils.commaSeparatedStringToAuthorityList("jane"));
	}

}
