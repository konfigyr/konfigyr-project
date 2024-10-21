package com.konfigyr.support;

import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Vladimir Spasic
 **/
class SlugTest {

	@Test
	@DisplayName("should create slug instance")
	void shouldCreateSlug() {
		final var slug = Slug.slugify("this is a slug");

		assertThat(slug)
				.hasToString("this-is-a-slug")
				.isEqualTo(Slug.slugify("this-is-a-slug"))
				.hasSameHashCodeAs(Slug.slugify("this-is-a-slug"))
				.extracting(Slug::get)
				.isEqualTo("this-is-a-slug");
	}

	@Test
	@DisplayName("should fail to create slug instance for null values")
	void shouldCheckNulls() {
		assertThatThrownBy(() -> Slug.slugify(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Value to slugify can not be null");
	}

	@Test
	@DisplayName("should fail to create slug instance for blank values")
	void shouldCheckBlanks() {
		assertThatThrownBy(() -> Slug.slugify("   "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Value to slugify can not be blank");
	}

	@Test
	@DisplayName("should fail to create slug instance for values longer than 255 characters")
	void shouldCheckSlugLength() {
		assertThatThrownBy(() -> Slug.slugify(RandomString.make(300)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Value to slugify is longer than 255 chars");
	}

	@ParameterizedTest(name = "value: \"{0}\"")
	@ValueSource(strings = {"/", "./", "../", "../../../", "$"})
	@DisplayName("should fail to create slugs from invalid values")
	void shouldValidate(String value) {
		assertThatThrownBy(() -> Slug.slugify(value))
				.isInstanceOf(IllegalArgumentException.class)
				.hasNoCause();
	}

	@MethodSource("forSlugs")
	@ParameterizedTest(name = "\"{0}\" -> {1}")
	@DisplayName("should create slugs from any non-blank string")
	void shouldCreateSlug(String value, String expected) {
		final var slug = Slug.slugify(value);

		assertThat(slug)
				.as("should generate a slug value of %s", expected)
				.extracting(Slug::get)
				.isEqualTo(expected);
	}

	static Stream<Arguments> forSlugs() {
		return Stream.of(
				Arguments.of("is a slug", "is-a-slug"),
				Arguments.of("../path", "path"),
				Arguments.of("/path/subpath", "path-subpath"),
				Arguments.of("path/../subpath", "path-subpath"),
				Arguments.of("a..%2fb", "a-2fb"),
				Arguments.of("a%2e%2e/b", "a-2e-2e-b"),
				Arguments.of("@domain.com", "domain-com"),
				Arguments.of("my/awesome/test", "my-awesome-test"),
				Arguments.of("my/awesome\\ntest", "my-awesome-ntest"),
				Arguments.of("/^(([a-z])+.)+[A-Z]([a-z])+$/i", "a-z-a-z-a-z-i"),
				Arguments.of("(?i)^(([a-z])+.)+[A-Z]([a-z])+$", "i-a-z-a-z-a-z"),
				Arguments.of("ä, ö, ü", "a-o-u"),
				Arguments.of("ЂЖљ ф Ц чЏш", "dzl-f-c-cds"),
				Arguments.of("Karađorđe", "karadorde"),
				Arguments.of("Ђорђе", "dorde")
		);
	}

	@MethodSource("forValidation")
	@ParameterizedTest(name = "\"{0}\" -> {1}")
	@DisplayName("should check if value is a valid slug")
	void shouldValidateSlugs(String value, boolean valid) {
		assertThat(Slug.isValid(value))
				.describedAs("value %s should be valid slug: %s", value, valid)
				.isEqualTo(valid);
	}

	static Stream<Arguments> forValidation() {
		return Stream.of(
				Arguments.of(null, false),
				Arguments.of("", false),
				Arguments.of("  ", false),
				Arguments.of("not a slug", false),
				Arguments.of("is-a-slug", true),
				Arguments.of("is-a-slug", true),
				Arguments.of("isaslug", true),
				Arguments.of("123456", true)
		);
	}
}