package com.konfigyr.test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * {@link TempDirFactory} implementation that creates an in-memory temporary directory via {@link Jimfs}
 * using the JUnit {@link TempDir} annotation.
 * <p>
 * Please note that only annotated fields or parameters of type {@link java.nio.file.Path} are supported as
 * {@code Jimfs} is a non-default file system, and {@link java.io.File} instances are associated with the
 * default file system only.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see TempDir
 */
public final class JimfsDirFactory implements TempDirFactory {

	private static final String DEFAULT_PREFIX = "junit-jimfs-";

	@Nullable
	private FileSystem fileSystem;

	private CleanupMode cleanupMode = CleanupMode.DEFAULT;

	/** {@inheritDoc} */
	@Override
	public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
			throws IOException {
		Optional<TempDir> annotation = elementContext.findAnnotation(TempDir.class);

		cleanupMode = annotation.map(TempDir::cleanup).orElse(CleanupMode.DEFAULT);
		fileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform());

		final Path root = fileSystem.getRootDirectories().iterator().next();
		return Files.createTempDirectory(root, DEFAULT_PREFIX);
	}

	@Override
	public void close() throws IOException {
		if (fileSystem != null && cleanupMode != CleanupMode.NEVER) {
			fileSystem.close();
			fileSystem = null;
		}
	}

}
