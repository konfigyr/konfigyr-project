package com.konfigyr.artifactory;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArtifactTest {

	@Test
	@DisplayName("should create artifact")
	void createArtifact() {
		final var artifact = Artifact.builder()
				.id(93645L)
				.groupId("com.konfigyr")
				.artifactId("konfigyr-api")
				.name("Konfigyr API")
				.description("Konfigyr REST API artifact")
				.website("https://api.konfigyr.com")
				.repository("https://github.com/konfigyr/konfigyr-project")
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();

		assertThat(artifact)
				.returns(EntityId.from(93645L), Artifact::id)
				.returns("com.konfigyr", Artifact::groupId)
				.returns("konfigyr-api", Artifact::artifactId)
				.returns("Konfigyr API", Artifact::name)
				.returns("Konfigyr REST API artifact", Artifact::description)
				.returns(URI.create("https://api.konfigyr.com"), Artifact::website)
				.returns(URI.create("https://github.com/konfigyr/konfigyr-project"), Artifact::repository);

		assertThat(artifact.createdAt())
				.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS));

		assertThat(artifact.updatedAt())
				.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS));
	}

	@Test
	@DisplayName("should create artifact from descriptor")
	void createArtifactFromDescriptor() {
		final var descriptor = mock(ArtifactDescriptor.class);
		doReturn("com.konfigyr").when(descriptor).groupId();
		doReturn("konfigyr-api").when(descriptor).artifactId();
		doReturn("Konfigyr API").when(descriptor).name();
		doReturn("Konfigyr REST API artifact").when(descriptor).description();
		doReturn(URI.create("https://api.konfigyr.com")).when(descriptor).website();
		doReturn(URI.create("https://github.com/konfigyr/konfigyr-project")).when(descriptor).repository();

		assertThat(Artifact.from(descriptor).id("000005KK96ZZP").build())
				.returns(EntityId.from(192846987254L), Artifact::id)
				.returns("com.konfigyr", Artifact::groupId)
				.returns("konfigyr-api", Artifact::artifactId)
				.returns("Konfigyr API", Artifact::name)
				.returns("Konfigyr REST API artifact", Artifact::description)
				.returns(URI.create("https://api.konfigyr.com"), Artifact::website)
				.returns(URI.create("https://github.com/konfigyr/konfigyr-project"), Artifact::repository)
				.returns(null, Artifact::createdAt)
				.returns(null, Artifact::updatedAt);

	}

	@Test
	@DisplayName("should sort artifacts by groupId and artifactId")
	void sortArtifacts() {
		final var builder = Artifact.builder().id(93645L);

		final var springMail = builder.groupId("org.springframework.boot")
				.artifactId("spring-boot-starter-mail")
				.build();
		final var springBatch = builder.groupId("org.springframework.boot")
				.artifactId("spring-boot-starter-batch")
				.build();
		final var konfigyrApi = builder.groupId("com.konfigyr")
				.artifactId("konfigyr-api")
				.build();
		final var konfigyrIdentity = builder.groupId("com.konfigyr")
				.artifactId("konfigyr-identity")
				.build();

		assertThat(Stream.of(konfigyrIdentity, springBatch, konfigyrApi, springMail).sorted())
				.containsExactly(konfigyrApi, konfigyrIdentity, springBatch, springMail);
	}

	@Test
	@DisplayName("should create versioned artifact from artifact descriptor")
	void createVersionedArtifactFromDescriptor() {
		final var artifact = Artifact.builder()
				.id(93645L)
				.groupId("com.konfigyr")
				.artifactId("konfigyr-api")
				.name("Konfigyr API")
				.description("Konfigyr REST API artifact")
				.website("https://api.konfigyr.com")
				.repository("https://github.com/konfigyr/konfigyr-project")
				.build();

		final var version = VersionedArtifact.from(artifact)
				.id("000005KK96ZZP")
				.artifact("000005KK96ZZP")
				.version("1.0.0")
				.checksum("checksum")
				.releasedAt(Instant.now())
				.build();

		assertThat(version)
				.returns(EntityId.from(192846987254L), VersionedArtifact::id)
				.returns(EntityId.from(192846987254L), VersionedArtifact::artifact)
				.returns(artifact.groupId(), VersionedArtifact::groupId)
				.returns(artifact.artifactId(), VersionedArtifact::artifactId)
				.returns(artifact.name(), VersionedArtifact::name)
				.returns(artifact.description(), VersionedArtifact::description)
				.returns(artifact.website(), VersionedArtifact::website)
				.returns(artifact.repository(), VersionedArtifact::repository)
				.returns("checksum", VersionedArtifact::checksum);

		assertThat(version.releasedAt())
				.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS));
	}
}
