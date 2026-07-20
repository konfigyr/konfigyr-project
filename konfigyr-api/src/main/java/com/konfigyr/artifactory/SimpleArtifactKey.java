package com.konfigyr.artifactory;

import com.fasterxml.jackson.annotation.JsonValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NullMarked
record SimpleArtifactKey(String groupId, String artifactId) implements ArtifactKey {

	@Serial
	private static final long serialVersionUID = 1L;

	static final Pattern PATTERN = Pattern.compile("^(?<groupId>[^:]+):(?<artifactId>[^:]+)$");

	static SimpleArtifactKey parse(@Nullable String key) {
		Assert.hasText(key, "Artifact key must not be null or blank");

		final Matcher matcher = PATTERN.matcher(key);

		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid Artifact key: " + key);
		}

		return new SimpleArtifactKey(matcher.group("groupId"), matcher.group("artifactId"));
	}

	SimpleArtifactKey {
		Assert.hasText(groupId, "Group ID cannot be empty");
		Assert.hasText(artifactId, "Artifact ID cannot be empty");
	}

	@Override
	@JsonValue
	public String toString() {
		return format();
	}
}
