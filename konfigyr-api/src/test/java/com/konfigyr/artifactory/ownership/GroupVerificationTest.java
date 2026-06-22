package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class GroupVerificationTest {

	@Test
	@DisplayName("should claim a groupId in pending state")
	void claim() {
		final var owner = Owner.of(EntityId.from(1), "john-doe");

		final var verification = GroupVerification.claim(owner, "com.mycompany");

		assertThat(verification)
				.returns(owner, GroupVerification::owner)
				.returns("com.mycompany", GroupVerification::groupId)
				.returns(VerificationState.PENDING, GroupVerification::state)
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.returns(null, GroupVerification::verifiedAt)
				.returns(null, GroupVerification::revokedAt);
	}

	@Test
	@DisplayName("should activate only pending verifications")
	void activate() {
		final var verification = GroupVerification.claim(Owner.of(EntityId.from(1), "john-doe"), "com.mycompany");

		final var active = verification.activate();

		assertThat(active)
				.returns(VerificationState.ACTIVE, GroupVerification::state)
				.returns(verification.id(), GroupVerification::id)
				.returns(verification.owner(), GroupVerification::owner)
				.returns(verification.groupId(), GroupVerification::groupId)
				.satisfies(it -> assertThat(it.verifiedAt()).isNotNull())
				.returns(null, GroupVerification::revokedAt);
	}

	@Test
	@DisplayName("should revoke active verifications")
	void revoke() {
		final var active = GroupVerification.claim(Owner.of(EntityId.from(1), "john-doe"), "com.mycompany")
				.activate();

		final var revoked = active.revoke();

		assertThat(revoked)
				.returns(VerificationState.REVOKED, GroupVerification::state)
				.returns(active.id(), GroupVerification::id)
				.returns(active.owner(), GroupVerification::owner)
				.returns(active.groupId(), GroupVerification::groupId)
				.returns(active.verifiedAt(), GroupVerification::verifiedAt)
				.satisfies(it -> assertThat(it.revokedAt()).isNotNull());
	}

	@Test
	@DisplayName("should reject invalid activate transitions")
	void invalidActivate() {
		final var active = GroupVerification.claim(Owner.of(EntityId.from(1), "john-doe"), "com.mycompany")
				.activate();
		final var revoked = active.revoke();

		assertThatThrownBy(active::activate)
				.isInstanceOf(VerificationChallengeNotFoundException.class)
				.hasMessageContaining("Cannot activate a ACTIVE verification");

		assertThatThrownBy(revoked::activate)
				.isInstanceOf(VerificationChallengeNotFoundException.class)
				.hasMessageContaining("Cannot activate a REVOKED verification");
	}

	@Test
	@DisplayName("should reject invalid revoke transitions")
	void invalidRevoke() {
		final var revoked = GroupVerification.claim(Owner.of(EntityId.from(1), "john-doe"), "com.mycompany")
				.activate()
				.revoke();

		assertThatThrownBy(revoked::revoke)
				.isInstanceOf(VerificationChallengeNotFoundException.class)
				.hasMessageContaining("Cannot revoke a REVOKED verification");
	}

}
