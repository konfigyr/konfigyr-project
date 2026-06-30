package com.konfigyr.vault;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.util.unit.DataUnit;

import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

@NullMarked
@RequiredArgsConstructor
class GitRepositoryHealthIndicator extends AbstractHealthIndicator {

	static final DataUnit[] DATA_UNITS = {
			DataUnit.KILOBYTES,
			DataUnit.MEGABYTES,
			DataUnit.GIGABYTES,
			DataUnit.TERABYTES
	};

	private final Path repositoryDirectory;

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		if (!Files.exists(repositoryDirectory)) {
			builder.down()
					.withDetail("directory", repositoryDirectory.toString())
					.withDetail("reason", "Repository directory does not exist");
			return;
		}

		if (!Files.isReadable(repositoryDirectory)) {
			builder.down()
					.withDetail("directory", repositoryDirectory.toString())
					.withDetail("reason", "Repository directory is not readable");
			return;
		}

		final FileStore store = Files.getFileStore(repositoryDirectory);

		builder.up()
				.withDetail("directory", repositoryDirectory.toString())
				.withDetail("usableSpace", formatBytes(store.getUsableSpace()))
				.withDetail("totalSpace", formatBytes(store.getTotalSpace()));
	}

	private static String formatBytes(long bytes) {
		if (bytes < 1024) {
			return bytes + " B";
		}

		int index = 0;
		double value = bytes / 1024.0;

		while (value >= 1024 && index < DATA_UNITS.length - 1) {
			value /= 1024;
			index++;
		}

		return String.format("%.2f %s", value, DATA_UNITS[index]);
	}

}
