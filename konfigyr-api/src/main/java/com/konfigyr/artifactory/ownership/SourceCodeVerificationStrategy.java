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

import java.net.URI;

/**
 * {@link VerificationStrategy} that proves ownership of a groupId by checking for a marker repository
 * on the source code host in the groupId.
 * <p>
 * The {@link SourceCodeHost host} (GitHub, GitLab or Bitbucket) and the repository owner are resolved
 * from the groupId, and the repository name is derived from the challenge token. The strategy then
 * issues an HTTP {@code GET} against the host's repository API: a successful response confirms that
 * the marker repository exists and therefore that the requester controls the account.
 * <p>
 * HTTP outcomes are mapped to {@link VerificationResult.FailureReason failure reasons} as follows:
 * <ul>
 *     <li>{@link VerificationResult.FailureReason#TARGET_NOT_FOUND TARGET_NOT_FOUND} – the repository
 *     does not exist ({@code 404});</li>
 *     <li>{@link VerificationResult.FailureReason#SERVICE_UNAVAILABLE SERVICE_UNAVAILABLE} – the host
 *     API returned a server error ({@code 5xx});</li>
 *     <li>{@link VerificationResult.FailureReason#INTERNAL_ERROR INTERNAL_ERROR} – any other client
 *     error ({@code 4xx}) or transport failure.</li>
 * </ul>
 *
 * @author Mila Zarkovic
 * @since 1.0.0
 * @see VerificationStrategy
 * @see VerificationMethod#SOURCE_CODE
 * @see SourceCodeHost
 */
@Slf4j
@NullMarked
@RequiredArgsConstructor
class SourceCodeVerificationStrategy implements VerificationStrategy {
	private static final Marker MARKER = MarkerFactory.getMarker("SOURCE_CODE_VERIFIER");

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

		final URI repoURI = host.toURI(groupId, challenge.token());

		try {
			restClient.get()
					.uri(repoURI)
					.retrieve()
					.toBodilessEntity();

			return VerificationResult.success(VerificationMethod.SOURCE_CODE);
		} catch (HttpClientErrorException.NotFound e) {
			log.warn(MARKER, "Repository not found: groupId={}, repo={}", groupId, repoURI);
			return VerificationResult.failure(VerificationResult.FailureReason.TARGET_NOT_FOUND);
		} catch (HttpClientErrorException e) {
			log.warn(MARKER, "Client error during verification: {}", e.getStatusCode(), e);
			return VerificationResult.failure(VerificationResult.FailureReason.INTERNAL_ERROR);
		} catch (HttpServerErrorException e) {
			log.warn(MARKER, "Server error during verification: {}", e.getStatusCode(), e);
			return VerificationResult.failure(VerificationResult.FailureReason.SERVICE_UNAVAILABLE);
		} catch (RestClientException e) {
			log.error(MARKER, "Source code verification failed for groupId={}, repo={}", groupId, repoURI, e);
			return VerificationResult.failure(VerificationResult.FailureReason.INTERNAL_ERROR);
		}
	}
}
