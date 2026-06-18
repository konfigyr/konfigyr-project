package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificationChallengeTest {

	@Test
	@DisplayName("should issue DNS challenge ")
	void issueDnsChallenge() {
		final var challenge = VerificationChallenge.issue(VerificationMethod.DNS);

		assertThat(challenge)
				.returns(VerificationMethod.DNS, VerificationChallenge::method)
				.returns(ChallengeState.UNVERIFIED, VerificationChallenge::state)
				.satisfies(it -> assertThat(it.token()).isNotBlank())
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.returns(null, VerificationChallenge::verifiedAt)
				.returns(null, VerificationChallenge::expiresAt);
	}

	@Test
	@DisplayName("should apply a success and failed challenge")
	void applyResult() {
		final var challenge = VerificationChallenge.issue(VerificationMethod.DNS);

		final var success = challenge.applyResult(VerificationResult.success(VerificationMethod.DNS));
		final var failed = challenge.applyResult(VerificationResult.failure("RECORD_NOT_FOUND"));

		assertThat(success)
				.returns(ChallengeState.VERIFIED, VerificationChallenge::state)
				.returns(challenge.id(), VerificationChallenge::id)
				.returns(challenge.method(), VerificationChallenge::method)
				.satisfies(it -> assertThat(it.verifiedAt()).isNotNull());

		assertThat(failed)
				.returns(ChallengeState.EXPIRED, VerificationChallenge::state)
				.returns(challenge.id(), VerificationChallenge::id)
				.returns(challenge.method(), VerificationChallenge::method)
				.returns(null, VerificationChallenge::verifiedAt);
	}

	@Test
	@DisplayName("should reject applying a result to a terminal challenge")
	void rejectTerminalChallenge() {
		final var challenge = VerificationChallenge.builder()
				.id(EntityId.generate().get())
				.verificationId(EntityId.from(1))
				.method(VerificationMethod.DNS)
				.token("token")
				.state(ChallengeState.EXPIRED)
				.createdAt(OffsetDateTime.now())
				.build();

		assertThatThrownBy(() -> challenge.applyResult(VerificationResult.success(VerificationMethod.DNS)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Cannot apply a result to a EXPIRED challenge");
	}

}
