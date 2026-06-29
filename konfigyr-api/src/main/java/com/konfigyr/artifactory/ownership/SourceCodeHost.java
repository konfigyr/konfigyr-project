package com.konfigyr.artifactory.ownership;

import org.jspecify.annotations.NullMarked;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Source code hosts supported by the source-code ownership verification strategy.
 *
 * <p>Ownership is proven by creating a public repository named {@code kfgyr-<token>} under the
 * account derived from the claimed {@code groupId}. The {@code groupId} is expected to follow the
 * {@code io.<host>.<owner>} convention, where {@code <host>} selects the host (see
 * {@link #fromGroupId(String)}) and {@code <owner>} is the account that must contain the repository.
 * {@link #toURI(String, String)} builds the host API URL used to assert that the repository exists.
 *
 * @author Mila Zarkovic
 * @since 1.0.0
 */
@NullMarked
public enum SourceCodeHost {

	/** GitHub source code host. */
	GITHUB("github", "https://api.github.com/repos/{owner}/kfgyr-{token}"),
	/**
	 * GitLab source code host.
	 * <p>The {@code encodedProject} must be URL-encoded as a single path segment
	 * (i.e. {@code project/repository} → {@code test-project%2Ftest-repo}).
	 */
	GITLAB("gitlab", "https://gitlab.com/api/v4/projects/{encodedProject}"),
	/** Bitbucket source code host. */
	BITBUCKET("bitbucket", "https://api.bitbucket.org/2.0/repositories/{owner}/kfgyr-{token}");

	private static final String REPO_NAME_PREFIX = "kfgyr-";

	private final String hostKey;
	private final String repoUrlTemplate;

	SourceCodeHost(String hostKey, String repoUrlTemplate) {
		this.hostKey = hostKey;
		this.repoUrlTemplate = repoUrlTemplate;
	}

	/**
	 * Builds the host API URI that locates the verification repository ({@code kfgyr-<token>})
	 * for the account derived from the given {@code groupId}.
	 *
	 * @param groupId claimed group id in the {@code io.<host>.<owner>} format
	 * @param token verification token that suffixes the repository name
	 * @return the host API URI pointing at the expected verification repository
	 */
	public URI toURI(String groupId, String token) {
		final String owner = resolveOwner(groupId);

		return switch (this) {
			case GITLAB -> {
				final String project = owner + "/" + REPO_NAME_PREFIX + token;
				yield UriComponentsBuilder.fromUriString(repoUrlTemplate)
						.uriVariables(Map.of("encodedProject", project))
						.encode()
						.build()
						.toUri();
			}
			default -> UriComponentsBuilder.fromUriString(repoUrlTemplate)
					.buildAndExpand(Map.of("owner", owner, "token", token))
					.toUri();
		};
	}

	/**
	 * Resolves the source code host from a claimed {@code groupId}.
	 *
	 * <p>The {@code groupId} must consist of exactly three non-blank, dot-separated segments
	 * starting with {@code io} (e.g. {@code io.github.acme}); the middle segment selects the host.
	 *
	 * @param groupId claimed group id in the {@code io.<host>.<owner>} format
	 * @return the matching host, or {@link Optional#empty()} when the id is malformed or unknown
	 */
	public static Optional<SourceCodeHost> fromGroupId(String groupId) {
		final String[] parts = groupId.split("\\.");

		if (parts.length != 3 || !"io".equals(parts[0]) || parts[1].isBlank() || parts[2].isBlank()) {
			return Optional.empty();
		}

		final String hostKey = parts[1];

		return Arrays.stream(values())
				.filter(h -> h.hostKey.equals(hostKey))
				.findFirst();
	}

	private String resolveOwner(String groupId) {
		final String[] parts = groupId.split("\\.");

		Assert.state(parts.length == 3, "Invalid groupId: " + groupId);

		return parts[2];
	}
}
