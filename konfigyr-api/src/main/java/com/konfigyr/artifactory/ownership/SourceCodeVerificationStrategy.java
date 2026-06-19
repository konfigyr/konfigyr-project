package com.konfigyr.artifactory.ownership;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@NullMarked
@RequiredArgsConstructor
class SourceCodeVerificationStrategy implements VerificationStrategy {
	private static final Marker MARKER = MarkerFactory.getMarker("SOURCE_CODE_VERIFIER");
	private static final String REPO_NAME_PREFIX = "";

	private final RestClient restClient;

	@Override
	public VerificationMethod method() {
		return VerificationMethod.SOURCE_CODE;
	}

	@Override
	public VerificationResult verify(GroupVerification verification, VerificationChallenge challenge) {
		final String groupId = verification.groupId();
		final SourceCodeHost host = SourceCodeHost.fromGroupId(groupId)
				.orElseThrow(() -> new IllegalStateException("No SourceCodeHost found for groupId: " + groupId));

		final String ownerPath = host.ownerPath(groupId);
		final String repoName = REPO_NAME_PREFIX + challenge.token();

		try {
			restClient.get()
					.uri(host.repoURI(ownerPath, repoName))
					.retrieve()
					.toBodilessEntity();

			return VerificationResult.success(VerificationMethod.SOURCE_CODE);
		} catch (HttpClientErrorException.NotFound e) {
			log.info(MARKER, "Repository not found: groupId={}, repo={}", groupId, repoName);
			return VerificationResult.failure(VerificationResult.FailureReason.TARGET_NOT_FOUND);
		} catch (HttpClientErrorException e) {
			log.warn(MARKER, "Client error during verification: {}", e.getStatusCode(), e);
			return VerificationResult.failure(VerificationResult.FailureReason.INTERNAL_ERROR);
		} catch (HttpServerErrorException e) {
			log.warn(MARKER, "Server error during verification: {}", e.getStatusCode(), e);
			return VerificationResult.failure(VerificationResult.FailureReason.SERVICE_UNAVAILABLE);
		} catch (RestClientException e) {
			log.error(MARKER, "Source code verification failed for groupId={}, repo={}", groupId, repoName, e);
			return VerificationResult.failure(VerificationResult.FailureReason.INTERNAL_ERROR);
		}
	}
}
