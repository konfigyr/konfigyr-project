package com.konfigyr.artifactory.ownership;

import org.jspecify.annotations.NullMarked;
import org.springframework.util.Assert;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

@NullMarked
public enum SourceCodeHost {

	GITHUB("github", "https://api.github.com/repos/%s/%s"),
	GITLAB("gitlab", "https://gitlab.com/api/v4/projects/%s"),
	BITBUCKET("bitbucket", "https://api.bitbucket.org/2.0/repositories/%s/%s");

	private final String hostKey;
	private final String repoUrlTemplate;

	SourceCodeHost(String hostKey, String repoUrlTemplate) {
		this.hostKey = hostKey;
		this.repoUrlTemplate = repoUrlTemplate;
	}

	public URI repoURI(String codeOwner, String repoName) {
		return switch (this) {
			case GITLAB -> URI.create(String.format(
					repoUrlTemplate,
					URLEncoder.encode(codeOwner + "/" + repoName, StandardCharsets.UTF_8)
			));
			default -> URI.create(String.format(repoUrlTemplate, codeOwner, repoName));
		};
	}

	public String ownerPath(String groupId) {
		final String[] parts = groupId.split("\\.");

		Assert.state(parts.length == 3, "Invalid groupId: " + groupId);

		return parts[2];
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
}
