package com.konfigyr.artifactory.ownership;

import com.konfigyr.artifactory.Owner;
import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SourceCodeVerificationStrategyTest {

	MockRestServiceServer server;
	SourceCodeVerificationStrategy strategy;

	@BeforeEach
	void setUp() {
		final RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		strategy = new SourceCodeVerificationStrategy(builder.build());
	}

	@Test
	@DisplayName("should expose SOURCE_CODE as the verification method")
	void method() {
		assertThat(strategy.method()).isEqualTo(VerificationMethod.SOURCE_CODE);
	}

	@Test
	@DisplayName("should return success when repository exists on GitHub")
	void verifyGitHubSuccess() {
		server.expect(requestTo("https://api.github.com/repos/alice/kfgyr-test-token"))
				.andRespond(withSuccess());

		assertThat(strategy.verify(verification("io.github.alice"), challenge("test-token")))
				.isEqualTo(VerificationResult.success(VerificationMethod.SOURCE_CODE));
	}

	@Test
	@DisplayName("should return success when repository exists on GitLab")
	void verifyGitLabSuccess() {
		server.expect(requestTo("https://gitlab.com/api/v4/projects/alice%2Fkfgyr-test-token"))
				.andRespond(withSuccess());

		assertThat(strategy.verify(verification("io.gitlab.alice"), challenge("test-token")))
				.isEqualTo(VerificationResult.success(VerificationMethod.SOURCE_CODE));
	}

	@Test
	@DisplayName("should return success when repository exists on Bitbucket")
	void verifyBitbucketSuccess() {
		server.expect(requestTo("https://api.bitbucket.org/2.0/repositories/alice/kfgyr-test-token"))
				.andRespond(withSuccess());

		assertThat(strategy.verify(verification("io.bitbucket.alice"), challenge("test-token")))
				.isEqualTo(VerificationResult.success(VerificationMethod.SOURCE_CODE));
	}

	@Test
	@DisplayName("should return TARGET_NOT_FOUND when repository does not exist")
	void verifyRepoNotFound() {
		server.expect(requestTo("https://api.github.com/repos/alice/kfgyr-test-token"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThat(strategy.verify(verification("io.github.alice"), challenge("test-token")))
				.isEqualTo(VerificationResult.failure(VerificationResult.FailureReason.TARGET_NOT_FOUND));
	}

	@Test
	@DisplayName("should return SERVICE_UNAVAILABLE when the host API returns 503")
	void verifyServiceUnavailable() {
		server.expect(requestTo("https://api.github.com/repos/alice/kfgyr-test-token"))
				.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

		assertThat(strategy.verify(verification("io.github.alice"), challenge("test-token")))
				.isEqualTo(VerificationResult.failure(VerificationResult.FailureReason.SERVICE_UNAVAILABLE));
	}

	@Test
	@DisplayName("should return SERVICE_UNAVAILABLE on any 5xx error")
	void verifyInternalErrorOn5xx() {
		server.expect(requestTo("https://api.github.com/repos/alice/kfgyr-500-error-token"))
				.andRespond(withServerError());

		assertThat(strategy.verify(verification("io.github.alice"), challenge("500-error-token")))
				.isEqualTo(VerificationResult.failure(VerificationResult.FailureReason.SERVICE_UNAVAILABLE));
	}

	@Test
	@DisplayName("should return INTERNAL_ERROR on unexpected 4xx error")
	void verifyInternalErrorOn4xx() {
		server.expect(requestTo("https://api.github.com/repos/alice/kfgyr-400-error-token"))
				.andRespond(withStatus(HttpStatus.FORBIDDEN));

		assertThat(strategy.verify(verification("io.github.alice"), challenge("400-error-token")))
				.isEqualTo(VerificationResult.failure(VerificationResult.FailureReason.INTERNAL_ERROR));
	}

	@Test
	@DisplayName("should return INTERNAL_ERROR on connection failure")
	void verifyInternalErrorOnConnectionFailure() {
		server.expect(requestTo("https://api.github.com/repos/alice/kfgyr-error-token"))
				.andRespond(_ -> {
					throw new IOException("Connection refused");
				});

		assertThat(strategy.verify(verification("io.github.alice"), challenge("error-token")))
				.isEqualTo(VerificationResult.failure(VerificationResult.FailureReason.INTERNAL_ERROR));
	}

	private static GroupVerification verification(String groupId) {
		return GroupVerification.builder()
				.id(1L)
				.owner(new Owner(EntityId.from(1L), "test-namespace"))
				.groupId(groupId)
				.state(VerificationState.PENDING)
				.build();
	}

	private static VerificationChallenge challenge(String token) {
		return VerificationChallenge.builder()
				.id(UUID.randomUUID())
				.verificationId(EntityId.from(1L))
				.method(VerificationMethod.SOURCE_CODE)
				.token(token)
				.state(ChallengeState.UNVERIFIED)
				.build();
	}
}
