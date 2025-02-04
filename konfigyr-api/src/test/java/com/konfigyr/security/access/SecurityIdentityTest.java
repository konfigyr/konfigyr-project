package com.konfigyr.security.access;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SecurityIdentityTest {

	@Test
	@DisplayName("security identities should be equal when entity identifiers match")
	void assertEquality() {
		assertThat(SecurityIdentity.of(EntityId.from(123)))
				.returns(EntityId.from(123).serialize(), SecurityIdentity::get)
				.isEqualTo(SecurityIdentity.of(EntityId.from(123)))
				.isNotEqualTo(SecurityIdentity.of(EntityId.from(321)));
	}

	@Test
	@DisplayName("security identities should have simple string representation")
	void assertStringRepresentation() {
		assertThat(SecurityIdentity.of(EntityId.from(123)))
				.hasToString("SecurityIdentity(%s)", EntityId.from(123));
	}

	@Test
	@DisplayName("should fail to create security identity with null identifiers")
	void assertEntityIdentifier() {
		assertThatIllegalArgumentException().isThrownBy(() -> SecurityIdentity.of(null));
	}

}
