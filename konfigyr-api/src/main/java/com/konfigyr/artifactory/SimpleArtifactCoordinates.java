package com.konfigyr.artifactory;

import com.fasterxml.jackson.annotation.JsonValue;
import com.konfigyr.version.Version;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;

import java.io.Serial;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record SimpleArtifactCoordinates(
		@NonNull String groupId,
		@NonNull String artifactId,
		@NonNull Version version
) implements ArtifactCoordinates {

	@Serial
	private static final long serialVersionUID = 1L;

	static final Pattern PATTERN = Pattern.compile("^(?<groupId>[^:]+):(?<artifactId>[^:]+):(?<version>[^:]+)$");

	static final Comparator<ArtifactCoordinates> COMPARATOR = Comparator
			.comparing(ArtifactCoordinates::groupId)
			.thenComparing(ArtifactCoordinates::artifactId)
			.thenComparing(ArtifactCoordinates::version);

	static SimpleArtifactCoordinates parse(String coordinates) {
		Assert.hasText(coordinates, "Artifact coordinates must not be null or blank");

		final Matcher matcher = PATTERN.matcher(coordinates);

		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid Artifact coordinates: " + coordinates);
		}

		return new SimpleArtifactCoordinates(
				matcher.group("groupId"),
				matcher.group("artifactId"),
				matcher.group("version")
		);
	}

	SimpleArtifactCoordinates(String groupId, String artifactId, String version) {
		this(groupId, artifactId, version == null ? null : Version.of(version));
	}

	SimpleArtifactCoordinates(String groupId, String artifactId, Version version) {
		Assert.hasText(groupId, "Group ID cannot be empty");
		Assert.hasText(artifactId, "Artifact ID cannot be empty");
		Assert.notNull(version, "Version cannot be null");
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

	@NonNull
	@Override
	@JsonValue
	public String toString() {
		return format();
	}
}
