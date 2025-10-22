package com.konfigyr.artifactory.store;

import com.konfigyr.artifactory.ArtifactCoordinates;
import com.konfigyr.test.JimfsDirFactory;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class FileSystemMetadataStoreTest {

	final ArtifactCoordinates coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-api", "1.0.0");

	@TempDir(factory = JimfsDirFactory.class)
	Path directory;

	MetadataStore store;

	@BeforeEach
	void setup() {
		store = new FileSystemMetadataStore(directory);
	}

	@Test
	@DisplayName("should store, retrieve and delete metadata from the file system")
	void storeRetrieveAndDelete() {
		final var contents = "some metadata contents";

		assertThat(store.save(coordinates, () -> new ByteArrayInputStream(contents.getBytes())))
				.isNotNull()
				.isInstanceOf(FileSystemResource.class)
				.returns("com.konfigyr:konfigyr-api:1.0.0.json", Resource::getFilename)
				.satisfies(resource -> assertThat(resource.getURI())
						.isEqualTo(directory.resolve("com.konfigyr:konfigyr-api:1.0.0.json").toUri())
				)
				.satisfies(resource -> assertThat(resource.getContentAsByteArray())
						.isEqualTo(contents.getBytes())
				);

		assertThat(store.get(coordinates))
				.isNotEmpty()
				.get(InstanceOfAssertFactories.type(FileSystemResource.class))
				.returns("com.konfigyr:konfigyr-api:1.0.0.json", Resource::getFilename)
				.satisfies(resource -> assertThat(resource.getURI())
						.isEqualTo(directory.resolve("com.konfigyr:konfigyr-api:1.0.0.json").toUri())
				)
				.satisfies(resource -> assertThat(resource.getContentAsByteArray())
						.isEqualTo(contents.getBytes())
				);

		assertThatNoException().isThrownBy(() -> store.remove(coordinates));

		assertThat(directory.resolve("com.konfigyr:konfigyr-api:1.0.0.json"))
				.returns(false, Files::exists);

	}

	@Test
	@DisplayName("should fail to store corrupt metadata input stream")
	void storeCorruptInputStream() {
		final InputStreamSource metadata = () -> {
			throw new FileSystemException("corrupt data");
		};

		assertThatExceptionOfType(UncheckedIOException.class)
				.isThrownBy(() -> store.save(coordinates, metadata))
				.withMessageContaining("Unexpected error occurred while storing metadata for: %s", coordinates)
				.withCauseInstanceOf(FileSystemException.class);
	}

	@Test
	@DisplayName("should retrieve unknown metadata from the store")
	void retrieveUnknown() {
		assertThat(store.get(coordinates))
				.isEmpty();
	}

	@Test
	@DisplayName("should delete unknown metadata from the store")
	void deleteUnknown() {
		assertThatNoException()
				.isThrownBy(() -> store.remove(coordinates));
	}

	@Test
	@DisplayName("should fail to delete corrupt metadata")
	void deleteCorrupt() throws IOException {
		final var path = directory.resolve(coordinates.format() + ".json");

		// forces an IO exception when the store metadata is a non-empty directory...
		Files.createDirectory(path);
		Files.createFile(path.resolve("corrupt.json"));

		assertThatExceptionOfType(UncheckedIOException.class)
				.isThrownBy(() -> store.remove(coordinates))
				.withMessageContaining("Unexpected error occurred while removing metadata for: %s", coordinates)
				.withCauseInstanceOf(DirectoryNotEmptyException.class);
	}

}
