package com.konfigyr.security.access;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SecurityIdentityTest {

	@Test
	@DisplayName("security identities should be equal when entity identifiers match")
	void assertAccountEquality() {
		assertThat(SecurityIdentity.account(EntityId.from(123)))
				.returns(EntityId.from(123).serialize(), SecurityIdentity::get)
				.isEqualTo(SecurityIdentity.account(EntityId.from(123)))
				.isNotEqualTo(SecurityIdentity.account(EntityId.from(321)));
	}

	@Test
	@DisplayName("security identities should be equal when client_id matches")
	void assertClientIdEquality() {
		assertThat(SecurityIdentity.oauthClient("client-id"))
				.returns("client-id", SecurityIdentity::get)
				.isEqualTo(SecurityIdentity.oauthClient("client-id"))
				.isNotEqualTo(SecurityIdentity.oauthClient("other-client-id"));
	}

	@Test
	@DisplayName("security identities should have simple string representation")
	void assertStringRepresentation() {
		assertThat(SecurityIdentity.account(EntityId.from(123)))
				.hasToString("SecurityIdentity(account=%s)", EntityId.from(123));

		assertThat(SecurityIdentity.oauthClient("client-id"))
				.hasToString("SecurityIdentity(client_id=%s)", "client-id");
	}

	@Test
	@DisplayName("should fail to create account based security identity with null identifiers")
	void assertAccountIdentifier() {
		assertThatIllegalArgumentException().isThrownBy(() -> SecurityIdentity.account(null));
	}

	@Test
	@DisplayName("should fail to create OAuth client based security identity with blank client_id")
	void assertClientIdentifier() {
		assertThatIllegalArgumentException().isThrownBy(() -> SecurityIdentity.oauthClient(null));
		assertThatIllegalArgumentException().isThrownBy(() -> SecurityIdentity.oauthClient(""));
		assertThatIllegalArgumentException().isThrownBy(() -> SecurityIdentity.oauthClient("  "));
	}

}
