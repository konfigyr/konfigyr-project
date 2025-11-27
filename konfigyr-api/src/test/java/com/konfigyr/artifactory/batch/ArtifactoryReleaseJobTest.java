package com.konfigyr.artifactory.batch;

import com.konfigyr.artifactory.ArtifactCoordinates;
import com.konfigyr.artifactory.Artifactory;
import com.konfigyr.artifactory.PropertyDefinition;
import com.konfigyr.artifactory.store.MetadataStore;
import com.konfigyr.test.AbstractIntegrationTest;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ListAssert;
import org.assertj.core.groups.Tuple;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.konfigyr.data.tables.ArtifactVersions.ARTIFACT_VERSIONS;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ArtifactoryReleaseJobTest extends AbstractIntegrationTest {

	@Autowired
	Artifactory artifactory;

	@Autowired
	DSLContext context;

	@Autowired
	JobOperator operator;

	@Autowired
	@Qualifier(ArtifactoryJobNames.RELEASE_JOB)
	Job job;

	@AfterAll
	static void cleanup(ApplicationContext context) {
		context.getBean(DSLContext.class).deleteFrom(ARTIFACT_VERSIONS)
				.where(ARTIFACT_VERSIONS.ARTIFACT_ID.eq(4L))
				.execute();
	}

	@Test
	@Order(0)
	@DisplayName("should validate if `artifact` job parameter is defined")
	void validateMissingArtifact() {
		assertThatExceptionOfType(InvalidJobParametersException.class)
				.isThrownBy(() -> operator.start(job, jobParametersBuilder().toJobParameters()))
				.withMessageContaining("do not contain required keys: [artifact]");
	}

	@Test
	@Order(1)
	@DisplayName("should fail to execute when versioned artifact does not exist")
	void unknownArtifact() throws Exception {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-unknown:1.0.0");

		mockArtifactMetadata(metadataStore, coordinates);

		final var execution = operator.start(job, jobParametersBuilder()
				.addString("artifact", coordinates.format(), true)
				.toJobParameters()
		);

		assertThat(execution.getExitStatus())
				.returns("FAILED", ExitStatus::getExitCode)
				.extracting(ExitStatus::getExitDescription, InstanceOfAssertFactories.STRING)
				.contains("Can not find artifact version with following coordinates: " + coordinates);
	}

	@Test
	@Order(2)
	@DisplayName("should fail to execute when metadata resource does not exist")
	void unknownResource() throws Exception {
		final var execution = operator.start(job, jobParametersBuilder()
				.addString("artifact", "com.konfigyr:konfigyr-resource:1.0.0", true)
				.toJobParameters()
		);

		assertThat(execution.getExitStatus())
				.returns("FAILED", ExitStatus::getExitCode)
				.extracting(ExitStatus::getExitDescription, InstanceOfAssertFactories.STRING)
				.contains("Could not find uploaded artifact property metadata for coordinates: com.konfigyr:konfigyr-resource:1.0.0");
	}

	@Test
	@Order(3)
	@DisplayName("should process the release job for 'com.konfigyr:konfigyr-licences:1.0.0'")
	void first() throws Exception {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-licences:1.0.0");

		mockArtifactMetadata(metadataStore, coordinates);
		createVersionedArtifact(context, coordinates.version().get());

		final var execution = operator.start(job, jobParametersBuilder()
				.addString("artifact", coordinates.format(), true)
				.toJobParameters()
		);

		assertThat(execution.getExitStatus())
				.returns("COMPLETED", ExitStatus::getExitCode);

		assertThatProperties(coordinates)
				.hasSize(5)
				.satisfies(containsProperties(
						tuple("konfigyr.licencing.enabled", 1, "1.0.0", "1.0.0"),
						tuple("konfigyr.licencing.types", 1, "1.0.0", "1.0.0"),
						tuple("konfigyr.licencing.licence-duration", 1, "1.0.0", "1.0.0"),
						tuple("konfigyr.licencing.cleanup.enabled", 1, "1.0.0", "1.0.0"),
						tuple("konfigyr.licencing.cleanup.cron", 1, "1.0.0", "1.0.0")
				));
	}

	@Test
	@Order(4)
	@DisplayName("should process the release job for 'com.konfigyr:konfigyr-licences:1.0.1'")
	void second() throws Exception {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-licences:1.0.1");

		mockArtifactMetadata(metadataStore, coordinates);
		createVersionedArtifact(context, coordinates.version().get());

		final var execution = operator.start(job, jobParametersBuilder()
				.addString("artifact", coordinates.format(), true)
				.toJobParameters()
		);

		assertThat(execution.getExitStatus())
				.returns("COMPLETED", ExitStatus::getExitCode);

		assertThatProperties(coordinates)
				.hasSize(5)
				.satisfies(containsProperties(
						tuple("konfigyr.licencing.enabled", 2, "1.0.0", "1.0.1"),
						tuple("konfigyr.licencing.types", 1, "1.0.1", "1.0.1"),
						tuple("konfigyr.licencing.licence-duration", 2, "1.0.0", "1.0.1"),
						tuple("konfigyr.licencing.cleanup.enabled", 2, "1.0.0", "1.0.1"),
						tuple("konfigyr.licencing.cleanup.cron", 2, "1.0.0", "1.0.1")
				));
	}

	@Test
	@Order(4)
	@DisplayName("should process the release job for 'com.konfigyr:konfigyr-licences:1.0.2'")
	void third() throws Exception {
		final var coordinates = ArtifactCoordinates.parse("com.konfigyr:konfigyr-licences:1.0.2");

		mockArtifactMetadata(metadataStore, coordinates);
		createVersionedArtifact(context, coordinates.version().get());

		final var execution = operator.start(job, jobParametersBuilder()
				.addString("artifact", coordinates.format(), true)
				.toJobParameters()
		);

		assertThat(execution.getExitStatus())
				.returns("COMPLETED", ExitStatus::getExitCode);

		assertThatProperties(coordinates)
				.hasSize(5)
				.satisfies(containsProperties(
						tuple("konfigyr.licencing.types", 2, "1.0.1", "1.0.2"),
						tuple("konfigyr.licencing.licence-duration", 1, "1.0.2", "1.0.2"),
						tuple("konfigyr.licencing.licence-expiry", 1, "1.0.2", "1.0.2"),
						tuple("konfigyr.licencing.cleanup.enabled", 3, "1.0.0", "1.0.2"),
						tuple("konfigyr.licencing.cleanup.cron", 3, "1.0.0", "1.0.2")
				));
	}

	ListAssert<PropertyDefinition> assertThatProperties(ArtifactCoordinates coordinates) {
		return assertThat(artifactory.properties(coordinates));
	}

	static Consumer<? super List<? extends PropertyDefinition>> containsProperties(Tuple... tuples) {
		return properties -> assertThat(properties)
				.extracting(
						PropertyDefinition::name,
						PropertyDefinition::occurrences,
						it -> it.firstSeen().get(),
						it -> it.lastSeen().get()
				)
				.containsExactlyInAnyOrder(tuples);
	}

	static void mockArtifactMetadata(MetadataStore store, ArtifactCoordinates coordinates) {
		final var resource = new ClassPathResource("artifact-metadata/%s.json".formatted(coordinates.format()));
		doReturn(Optional.of(resource)).when(store).get(coordinates);
	}

	static void createVersionedArtifact(DSLContext context, String version) {
		context.insertInto(ARTIFACT_VERSIONS)
				.set(ARTIFACT_VERSIONS.ARTIFACT_ID, 4L)
				.set(ARTIFACT_VERSIONS.VERSION, version)
				.execute();
	}

	static JobParametersBuilder	jobParametersBuilder() {
		return new JobParametersBuilder();
	}

}
