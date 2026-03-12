package com.konfigyr.artifactory;

import org.apache.commons.lang3.function.Consumers;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.function.Consumer;

/***
 * Utility class that contains test {@link Artifact}, {@link ArtifactMetadata}
 * and {@link PropertyDescriptor} stubs.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@NullMarked
public interface TestArtifacts {

	/**
	 * Creates a test {@code com.konfigyr:konfigyr-id:1.0.0} artifact using the default
	 * {@link DefaultArtifact} implementation.
	 *
	 * @return the test artifact, never {@literal null}
	 * @see DefaultArtifact
	 */
	static Artifact artifact() {
		return artifact(ArtifactCoordinates.of("com.konfigyr", "konfigyr-id", "1.0.0"));
	}

	/**
	 * Creates a test {@code com.konfigyr:konfigyr-id:1.0.0} artifact using the default
	 * {@link DefaultArtifact} implementation and a builder customizer.
	 *
	 * @param consumer the builder customizer, can't be {@literal null}
	 * @return the test artifact, never {@literal null}
	 * @see DefaultArtifact
	 */
	static Artifact artifact(Consumer<DefaultArtifact.Builder> consumer) {
		return artifact(ArtifactCoordinates.of("com.konfigyr", "konfigyr-id", "1.0.0"), consumer);
	}

	/**
	 * Creates an artifact using the default {@link DefaultArtifact} implementation based on
	 * the given {@link ArtifactCoordinates}.
	 *
	 * @param coordinates the coordinates to be used to create the artifact, can't be {@literal null}
	 * @return the test artifact, never {@literal null}
	 * @see DefaultArtifact
	 */
	static Artifact artifact(ArtifactCoordinates coordinates) {
		return artifact(coordinates, Consumers.nop());
	}

	/**
	 * Creates an artifact using the default {@link DefaultArtifact} implementation based on
	 * the given {@link ArtifactCoordinates}.
	 * <p>
	 * You can use the {@code consumer} to customize the artifact that would be created.
	 *
	 * @param coordinates the coordinates to be used to create the artifact, can't be {@literal null}
	 * @param consumer the builder customizer, can't be {@literal null}
	 * @return the test artifact, never {@literal null}
	 * @see DefaultArtifact
	 */
	static Artifact artifact(ArtifactCoordinates coordinates, Consumer<DefaultArtifact.Builder> consumer) {
		final var builder = Artifact.builder()
				.groupId(coordinates.groupId())
				.artifactId(coordinates.artifactId())
				.version(coordinates.version().get());

		consumer.accept(builder);

		return builder.build();
	}

	/**
	 * Creates a test {@code com.konfigyr:konfigyr-id:1.0.0} artifact metadata using the default
	 * {@link DefaultArtifactMetadata} implementation with the predefined set of {@link PropertyDescriptor}s.
	 *
	 * @return the test artifact metadata, never {@literal null}
	 * @see DefaultArtifactMetadata
	 */
	static ArtifactMetadata metadata() {
		return metadata(artifact());
	}

	/**
	 * Creates a test artifact metadata from the supplied {@link ArtifactCoordinates} with the predefined
	 * set of {@link PropertyDescriptor}s.
	 *
	 * @param coordinates the coordinates to be used to create the artifact metadata, can't be {@literal null}
	 * @return the test artifact metadata, never {@literal null}
	 * @see DefaultArtifactMetadata
	 */
	static ArtifactMetadata metadata(ArtifactCoordinates coordinates) {
		return metadata(artifact(coordinates));
	}

	/**
	 * Creates a test artifact metadata from the supplied {@link Artifact} with the predefined
	 * set of {@link PropertyDescriptor}s.
	 *
	 * @param artifact the artifact to be used to create the artifact metadata, can't be {@literal null}
	 * @return the test artifact metadata, never {@literal null}
	 * @see DefaultArtifactMetadata
	 */
	static ArtifactMetadata metadata(Artifact artifact) {
		return metadata(
				artifact,
				PropertyDescriptor.builder()
						.name("konfigyr.identity.authorization.client-id")
						.description("Client ID of the built-in Konfigyr OAuth client.")
						.typeName("java.lang.String")
						.schema(StringSchema.instance())
						.defaultValue("konfigyr")
						.build(),
				PropertyDescriptor.builder()
						.name("konfigyr.identity.authorization.client-secret")
						.description("Client secret of the built-in Konfigyr OAuth client. May not be left blank.")
						.typeName("java.lang.String")
						.schema(StringSchema.instance())
						.build(),
				PropertyDescriptor.builder()
						.name("konfigyr.identity.authorization.token.access-token-time-to-live")
						.description("Time-to-live for an access token.")
						.typeName("java.time.Duration")
						.schema(StringSchema.builder().format("duration").build())
						.defaultValue("5m")
						.build(),
				PropertyDescriptor.builder()
						.name("konfigyr.crypto.master-key.value")
						.description("Base 64 encoded value of the Key Encryption Key (KEK) used by the Konfigyr application.")
						.typeName("java.lang.String")
						.schema(StringSchema.instance())
						.build()
		);
	}

	/**
	 * Creates a test {@code com.konfigyr:konfigyr-id:1.0.0} artifact metadata using the default
	 * {@link DefaultArtifactMetadata} implementation and given collection of {@link PropertyDescriptor}s.
	 *
	 * @param properties the properties to be added to the artifact metadata
	 * @return the test artifact metadata, never {@literal null}
	 * @see DefaultArtifactMetadata
	 */
	static ArtifactMetadata metadata(PropertyDescriptor... properties) {
		return metadata(artifact(), properties);
	}

	/**
	 * Creates a test artifact metadata using the specified {@link ArtifactCoordinates} and
	 * given collection of {@link PropertyDescriptor}s.
	 *
	 * @param coordinates the coordinates to be used to create the artifact metadata
	 * @param properties the properties to be added to the artifact metadata
	 * @return the test artifact metadata, never {@literal null}
	 */
	static ArtifactMetadata metadata(ArtifactCoordinates coordinates, PropertyDescriptor... properties) {
		return metadata(artifact(coordinates), properties);
	}

	/**
	 * Creates a test artifact metadata from the supplied {@link Artifact} and given collection
	 * of {@link PropertyDescriptor}s.
	 *
	 * @param artifact the artifact to be used to create the artifact metadata, can't be {@literal null}
	 * @param properties the properties to be added to the artifact metadata
	 * @return the test artifact metadata, never {@literal null}
	 * @see DefaultArtifactMetadata
	 */
	static ArtifactMetadata metadata(Artifact artifact, PropertyDescriptor... properties) {
		return artifact.toMetadata(List.of(properties));
	}

}
