package com.konfigyr.artifactory.ownership;

import org.jspecify.annotations.NullMarked;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

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
