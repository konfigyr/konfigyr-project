package com.konfigyr.vault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GitRepositoryHealthIndicatorTest {

	@Test
	@DisplayName("should report UP when repository directory exists and is readable")
	void shouldReportUp(@TempDir Path directory) {
		final var indicator = new GitRepositoryHealthIndicator(directory);
		final var health = indicator.health(true);

		assertThat(health)
				.isNotNull()
				.returns(Status.UP, Health::getStatus);

		assertThat(health.getDetails())
				.containsEntry("directory", directory.toString())
				.containsKey("usableSpace")
				.containsKey("totalSpace");
	}

	@Test
	@DisplayName("should report DOWN when repository directory does not exist")
	void shouldReportDownWhenDirectoryDoesNotExist(@TempDir Path directory) {
		final var missing = directory.resolve("nonexistent");
		final var indicator = new GitRepositoryHealthIndicator(missing);
		final var health = indicator.health(true);

		assertThat(health)
				.isNotNull()
				.returns(Status.DOWN, Health::getStatus);

		assertThat(health.getDetails())
				.containsEntry("directory", missing.toString())
				.containsEntry("reason", "Repository directory does not exist");
	}

	@Test
	@DisplayName("should report DOWN when repository directory is not readable")
	void shouldReportDownWhenDirectoryIsNotReadable(@TempDir Path directory) throws Exception {
		final var restricted = Files.createDirectory(directory.resolve("restricted"));

		assertThat(restricted.toFile().setReadable(false))
				.as("Failed to restrict repository directory access")
				.isTrue();

		try {
			final var indicator = new GitRepositoryHealthIndicator(restricted);
			final var health = indicator.health(true);

			assertThat(health)
					.isNotNull()
					.returns(Status.DOWN, Health::getStatus);

			assertThat(health.getDetails())
					.containsEntry("directory", restricted.toString())
					.containsEntry("reason", "Repository directory is not readable");
		} finally {
			assertThat(restricted.toFile().setReadable(true))
					.as("Failed to restore repository directory access")
					.isTrue();
		}
	}

	@Test
	@DisplayName("should format disk space in human-readable units")
	void shouldFormatDiskSpace(@TempDir Path directory) {
		final var indicator = new GitRepositoryHealthIndicator(directory);
		final var health = indicator.health(true);

		assertThat(health)
				.isNotNull()
				.returns(Status.UP, Health::getStatus);

		assertThat(health.getDetails().get("usableSpace"))
				.asString()
				.matches("\\d+(\\.\\d+)? (B|KILOBYTES|MEGABYTES|GIGABYTES|TERABYTES)");

		assertThat(health.getDetails().get("totalSpace"))
				.asString()
				.matches("\\d+(\\.\\d+)? (B|KILOBYTES|MEGABYTES|GIGABYTES|TERABYTES)");
	}

}
