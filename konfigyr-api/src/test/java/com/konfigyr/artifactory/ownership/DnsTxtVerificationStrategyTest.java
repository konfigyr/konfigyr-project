package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedConstruction;

import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.InitialDirContext;

import static com.konfigyr.artifactory.ownership.VerificationResult.FailureReason.*;
import static com.konfigyr.artifactory.ownership.VerificationStrategyTestUtils.mockDnsException;
import static org.assertj.core.api.Assertions.*;

class DnsTxtVerificationStrategyTest {

	final DnsTxtVerificationStrategy strategy = new DnsTxtVerificationStrategy();

	@Test
	@DisplayName("should expose DNS as the verification method")
	void method() {
		assertThat(strategy.method()).isEqualTo(VerificationMethod.DNS);
	}

	@ParameterizedTest(name = "{0} → {1}")
	@CsvSource({
			"com.mycompany,          mycompany.com",
			"org.apache.commons,     apache.org",
			"io.github.user.project, github.io"
	})
	@DisplayName("should return success when one of multiple TXT records matches")
	void verifySuccessfullyTXTRecord(String groupId, String expectedDomain) {
		final GroupVerification verification = verification(groupId);
		final VerificationChallenge challenge = challenge("test-token");

		try (MockedConstruction<InitialDirContext> ignored = VerificationStrategyTestUtils.mockDns(expectedDomain, "some-other-record", "konfigyr-verification=test-token")) {
			assertThat(strategy.verify(verification, challenge))
					.isEqualTo(VerificationResult.success(VerificationMethod.DNS));
		}
	}

	@Test
	@DisplayName("should reject invalid groupId")
	void verifyInvalidGroup() {
		final GroupVerification verification = verification("com");
		final VerificationChallenge challenge = challenge("invalid-token");

		assertThatIllegalArgumentException()
				.isThrownBy(() -> strategy.verify(verification, challenge))
				.withMessageContaining("com");
	}

	@Test
	@DisplayName("should return TARGET_NOT_FOUND when domain has no TXT attribute")
	void verifyDomainWithNoTxtAttribute() {
		final GroupVerification verification = verification("com.mycompany");
		final VerificationChallenge challenge = challenge("abc123");

		try (MockedConstruction<InitialDirContext> ignored = VerificationStrategyTestUtils.mockDnsNoTxt("mycompany.com")) {
			assertThat(strategy.verify(verification, challenge))
					.isEqualTo(VerificationResult.failure(TARGET_NOT_FOUND));
		}
	}

	// integration test
	@Test
	@DisplayName("should return TOKEN_MISMATCH for a domain that exists but has wrong token")
	void verifyDomainWithWrongToken() {
		final GroupVerification verification = verification("com.google");
		final VerificationChallenge challenge = challenge("wrong-token");

		final VerificationResult result = strategy.verify(verification, challenge);

		assertThat(result).isEqualTo(VerificationResult.failure(TOKEN_MISMATCH));
	}

	// integration test
	@Test
	@DisplayName("should return TARGET_NOT_FOUND for a non-existing domain derived from an invalid groupId")
	void verifyNonExistingDomain() {
		final GroupVerification verification = verification("com.domain-does-not-exist-konfigyr");
		final VerificationChallenge challenge = challenge("any-token");

		final VerificationResult result = strategy.verify(verification, challenge);

		assertThat(result).isEqualTo(VerificationResult.failure(TARGET_NOT_FOUND));
	}

	@Test
	@DisplayName("should return SERVICE_UNAVAILABLE when DNS lookup times out")
	void verifyTimeout() {
		final GroupVerification verification = verification("com.mycompany");
		final VerificationChallenge challenge = challenge("abc123");

		try (MockedConstruction<InitialDirContext> ignored = mockDnsException(new CommunicationException("DNS timed out"))) {
			assertThat(strategy.verify(verification, challenge))
					.isEqualTo(VerificationResult.failure(SERVICE_UNAVAILABLE));
		}
	}

	@Test
	@DisplayName("should return SERVICE_UNAVAILABLE when DNS service is unavailable")
	void verifyServiceUnavailable() {
		final GroupVerification verification = verification("com.mycompany");
		final VerificationChallenge challenge = challenge("abc123");

		try (MockedConstruction<InitialDirContext> ignored = mockDnsException(new ServiceUnavailableException("DNS service unavailable"))) {
			assertThat(strategy.verify(verification, challenge))
					.isEqualTo(VerificationResult.failure(SERVICE_UNAVAILABLE));
		}
	}

	@Test
	@DisplayName("should return INTERNAL_ERROR when a NamingException is raised")
	void verifyNamingException() {
		final GroupVerification verification = verification("com.mycompany");
		final VerificationChallenge challenge = challenge("abc123");

		try (MockedConstruction<InitialDirContext> ignored = mockDnsException(new NamingException("resolution failure"))) {
			assertThat(strategy.verify(verification, challenge))
					.isEqualTo(VerificationResult.failure(INTERNAL_ERROR));
		}
	}

	@Test
	@DisplayName("should not propagate NamingException as unchecked exception")
	void verifyDoesNotPropagateException() {
		final GroupVerification verification = verification("com.mycompany");
		final VerificationChallenge challenge = challenge("abc123");

		try (MockedConstruction<InitialDirContext> ignored = mockDnsException(new NamingException("boom"))) {
			assertThatNoException().isThrownBy(() -> strategy.verify(verification, challenge));
		}
	}

	private static GroupVerification verification(String groupId) {
		return GroupVerification.builder()
				.owner(Owner.of(EntityId.from(1L), "test-namespace"))
				.groupId(groupId)
				.state(VerificationState.PENDING)
				.build();
	}

	private static VerificationChallenge challenge(String token) {
		return VerificationChallenge.builder()
				.method(VerificationMethod.DNS)
				.token(token)
				.state(ChallengeState.UNVERIFIED)
				.build();
	}
}
