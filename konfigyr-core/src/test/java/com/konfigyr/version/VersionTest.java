package com.konfigyr.version;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.exc.ValueInstantiationException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Year;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class VersionTest {

	final static JsonMapper mapper = new JsonMapper();

	@Test
	@DisplayName("should fail to null, empty or blank version representations")
	void parseInvalidVersion() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Version.of(null))
				.withMessageContaining("Version must not be empty");

		assertThatIllegalArgumentException()
				.isThrownBy(() -> Version.of(""))
				.withMessageContaining("Version must not be empty");

		assertThatIllegalArgumentException()
				.isThrownBy(() -> Version.of("   "))
				.withMessageContaining("Version must not be empty");
	}

	@Test
	@DisplayName("should create empty metadata version")
	void createEmptyMetadataVersion() {
		assertThat(MetadataVersion.EMPTY)
				.isSameAs(MetadataVersion.of())
				.isEqualTo(MetadataVersion.of(""))
				.hasSameHashCodeAs(MetadataVersion.of());
	}

	@Test
	@DisplayName("should check equality and hash code of version metadata")
	void assertMetadataVersionEquality() {
		assertThat(MetadataVersion.of("alpha", "1"))
				.hasToString("MetadataVersion([alpha, 1])")
				.isEqualTo(MetadataVersion.of("alpha.1"))
				.hasSameHashCodeAs(MetadataVersion.of("alpha.1"))
				.isNotEqualTo(MetadataVersion.of("alpha.2"))
				.doesNotHaveSameHashCodeAs(MetadataVersion.of("alpha.2"));
	}

	@Test
	@DisplayName("should sort metadata versions")
	void sortMetadataVersion() {
		final var versions = Stream.of("", "alpha.beta.1", "alpha.1", "beta.1", "alpha.beta.3", "alpha.11", "alpha.2", "alpha.beta", "beta.11")
				.map(MetadataVersion::of)
				.sorted()
				.toList();

		assertThat(versions).containsExactly(
				MetadataVersion.of("alpha", "1"),
				MetadataVersion.of("alpha", "2"),
				MetadataVersion.of("alpha", "11"),
				MetadataVersion.of("alpha", "beta"),
				MetadataVersion.of("alpha", "beta", "1"),
				MetadataVersion.of("alpha", "beta", "3"),
				MetadataVersion.of("beta", "1"),
				MetadataVersion.of("beta", "11"),
				MetadataVersion.EMPTY
		);
	}

	@Test
	@DisplayName("should parse simple semantic version")
	void parseSemanticVersion() {
		assertThat(Version.of("24.12.5"))
				.isInstanceOf(SemanticVersion.class)
				.asInstanceOf(InstanceOfAssertFactories.type(SemanticVersion.class))
				.returns("24.12.5", SemanticVersion::original)
				.returns(24, SemanticVersion::major)
				.returns(12, SemanticVersion::minor)
				.returns(5, SemanticVersion::patch)
				.returns(MetadataVersion.EMPTY, SemanticVersion::preRelease)
				.returns(MetadataVersion.EMPTY, SemanticVersion::build)
				.hasToString("SemanticVersion(24.12.5)")
				.isEqualTo(new SemanticVersion(24, 12, 5, "24.12.5"))
				.hasSameHashCodeAs(new SemanticVersion(24, 12, 5, "24.12.5"));
	}

	@Test
	@DisplayName("should parse semantic version with pre release")
	void parseSemanticVersionWithPreRelease() {
		assertThat(Version.of(" 24.12.5-RC1 "))
				.isInstanceOf(SemanticVersion.class)
				.asInstanceOf(InstanceOfAssertFactories.type(SemanticVersion.class))
				.returns("24.12.5-RC1", SemanticVersion::original)
				.returns(24, SemanticVersion::major)
				.returns(12, SemanticVersion::minor)
				.returns(5, SemanticVersion::patch)
				.returns(MetadataVersion.of("RC1"), SemanticVersion::preRelease)
				.returns(MetadataVersion.EMPTY, SemanticVersion::build)
				.hasToString("SemanticVersion(24.12.5-RC1)")
				.isEqualTo(new SemanticVersion(24, 12, 5, MetadataVersion.of("RC1"), MetadataVersion.EMPTY, "24.12.5-RC1"))
				.hasSameHashCodeAs(new SemanticVersion(24, 12, 5, MetadataVersion.of("RC1"), MetadataVersion.EMPTY, "24.12.5-RC1"));
	}

	@Test
	@DisplayName("should parse semantic version with build information")
	void parseSemanticVersionWithBuildInformation() {
		assertThat(Version.of("v1.0.1+metadata"))
				.isInstanceOf(SemanticVersion.class)
				.asInstanceOf(InstanceOfAssertFactories.type(SemanticVersion.class))
				.returns("v1.0.1+metadata", SemanticVersion::original)
				.returns(1, SemanticVersion::major)
				.returns(0, SemanticVersion::minor)
				.returns(1, SemanticVersion::patch)
				.returns(MetadataVersion.EMPTY, SemanticVersion::preRelease)
				.returns(MetadataVersion.of("metadata"), SemanticVersion::build)
				.hasToString("SemanticVersion(v1.0.1+metadata)");
	}

	@Test
	@DisplayName("should parse semantic version with pre release and build information")
	void parseExtendedSemanticVersion() {
		assertThat(Version.of("2.1.0-alpha.1+build.123"))
				.isInstanceOf(SemanticVersion.class)
				.asInstanceOf(InstanceOfAssertFactories.type(SemanticVersion.class))
				.returns("2.1.0-alpha.1+build.123", SemanticVersion::original)
				.returns(2, SemanticVersion::major)
				.returns(1, SemanticVersion::minor)
				.returns(0, SemanticVersion::patch)
				.returns(MetadataVersion.of("alpha", "1"), SemanticVersion::preRelease)
				.returns(MetadataVersion.of("build", "123"), SemanticVersion::build)
				.hasToString("SemanticVersion(2.1.0-alpha.1+build.123)");
	}

	@Test
	@DisplayName("should sort semantic versions")
	void sortSemanticVersions() {
		assertThatVersions(
				"1.0.0-rc.1", "1.1.1", "1.0.0-alpha.beta", "2.0.0-alpha", "1.0.0-rc.2", "1.0.0-alpha",
				"1.0.0-rc.1+build.100", "1.0.0", "1.0.0-rc.10", "1.0.1", "2.0.0"
		).containsExactly(
				"1.0.0-alpha", "1.0.0-alpha.beta", "1.0.0-rc.1", "1.0.0-rc.1+build.100",
				"1.0.0-rc.2", "1.0.0-rc.10", "1.0.0", "1.0.1", "1.1.1", "2.0.0-alpha", "2.0.0"
		);
	}

	@Test
	@DisplayName("should parse calendar version without day information")
	void parseCalendarVersionWithoutDay() {
		assertThat(Version.of("180.10"))
				.isInstanceOf(CalendarVersion.class)
				.asInstanceOf(InstanceOfAssertFactories.type(CalendarVersion.class))
				.returns("180.10", CalendarVersion::original)
				.returns(Year.of(180), CalendarVersion::year)
				.returns(10, CalendarVersion::primary)
				.returns(-1, CalendarVersion::secondary)
				.returns(null, CalendarVersion::modifier)
				.hasToString("CalendarVersion(180.10)");
	}

	@Test
	@DisplayName("should parse simple calendar version")
	void parseCalendarVersion() {
		assertThat(Version.of("24-03-05"))
				.isInstanceOf(CalendarVersion.class)
				.asInstanceOf(InstanceOfAssertFactories.type(CalendarVersion.class))
				.returns("24-03-05", CalendarVersion::original)
				.returns(Year.of(2024), CalendarVersion::year)
				.returns(3, CalendarVersion::primary)
				.returns(5, CalendarVersion::secondary)
				.returns(null, CalendarVersion::modifier);
	}

	@Test
	@DisplayName("should parse calendar version with modifier")
	void parseCalendarVersionWithModifier() {
		assertThat(Version.of("2024.01-dev"))
				.isInstanceOf(CalendarVersion.class)
				.asInstanceOf(InstanceOfAssertFactories.type(CalendarVersion.class))
				.returns("2024.01-dev", CalendarVersion::original)
				.returns(Year.of(2024), CalendarVersion::year)
				.returns(1, CalendarVersion::primary)
				.returns(-1, CalendarVersion::secondary)
				.returns("dev", CalendarVersion::modifier);
	}

	@Test
	@DisplayName("should sort calendar versions")
	void sortCalendarVersions() {
		assertThatVersions(
				"24.01", "2023.12", "24-01-1-alpha", "2024-01-01", "23.12", "2024.01-dev", "2023.12-beta", "2024.01-rc"
		).containsExactly(
				"2023.12-beta", "2023.12", "23.12", "2024.01-dev", "2024.01-rc", "24.01", "24-01-1-alpha", "2024-01-01"
		);
	}

	@MethodSource("unknowns")
	@DisplayName("should parse unknown version")
	@ParameterizedTest(name = "should parse unknown version: {0}")
	void parseUnknownVersion(String version, Version expected) {
		assertThat(Version.of(version))
				.isInstanceOf(UnknownVersion.class)
				.isEqualTo(expected)
				.returns(version, Version::get)
				.hasToString("UnknownVersion(%s)", version);
	}

	@Test
	@DisplayName("should sort unknown versions")
	void sortUnknownVersions() {
		assertThatVersions("r117", "20240325.1", "1.2.x", "2024.x", "invalid")
				.containsExactly("1.2.x", "2024.x", "20240325.1", "invalid", "r117");
	}

	static Stream<Arguments> unknowns() {
		return Stream.of(
				Arguments.of("r117", new UnknownVersion("r117")),
				Arguments.of("20240325.1", new UnknownVersion("20240325.1")),
				Arguments.of("1.2.x", new UnknownVersion("1.2.x")),
				Arguments.of("2024.x", new UnknownVersion("2024.x")),
				Arguments.of("invalid", new UnknownVersion("invalid"))
		);
	}

	@Test
	@DisplayName("should serialize version to a JSON text node")
	void serializeToJson() {
		final var version = Version.of("2025.01-dev");

		assertThat(mapper.writeValueAsString(version))
				.isEqualTo("\"2025.01-dev\"");
	}

	@Test
	@DisplayName("should deserialize version from a JSON text node")
	void deserializeFromJson() {
		assertThat(mapper.readValue("\"2025.01-dev\"", Version.class))
				.isEqualTo(Version.of("2025.01-dev"));
	}

	@Test
	@DisplayName("should fail to deserialize version from an empty JSON text node")
	void deserializeFromEmptyJsonText() {
		assertThatExceptionOfType(ValueInstantiationException.class)
				.isThrownBy(() -> mapper.readValue("\"\"", Version.class))
				.havingRootCause()
				.withMessageContaining("Version must not be empty");
	}

	static ListAssert<String> assertThatVersions(String... versions) {
		return assertThat(
				Stream.of(versions)
						.map(Version::of)
						.sorted()
						.map(Version::get)
						.toList()
		);
	}

}
