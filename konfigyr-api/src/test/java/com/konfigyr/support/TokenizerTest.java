package com.konfigyr.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.assertj.core.api.Assertions.*;

class TokenizerTest {

	@Test
	@DisplayName("should treat the entire input as a single token via identity")
	void shouldTokenizeIdentity() {
		assertThat(Tokenizer.identity().tokenize("hello world").toList())
				.containsExactly("hello world");
	}

	@Test
	@DisplayName("should treat blank input as a single empty token via identity")
	void shouldTokenizeIdentityForBlankInput() {
		assertThat(Tokenizer.identity().tokenize("").toList())
				.containsExactly("");
	}

	@Test
	@DisplayName("should split on runs of whitespace")
	void shouldTokenizeOnWhitespace() {
		assertThat(Tokenizer.whitespace().tokenize("hello   world foo").toList())
				.containsExactly("hello", "world", "foo");
	}

	@Test
	@DisplayName("should ignore leading and trailing whitespace before splitting")
	void shouldIgnoreSurroundingWhitespace() {
		assertThat(Tokenizer.whitespace().tokenize("  hello world  ").toList())
				.containsExactly("hello", "world");
	}

	@Test
	@DisplayName("should produce a single empty token for blank input")
	void shouldTokenizeBlankInputToSingleEmptyToken() {
		assertThat(Tokenizer.whitespace().tokenize("   ").toList())
				.containsExactly("");
	}

	@Test
	@DisplayName("should split a dotted or hyphenated identifier into its alphanumeric runs")
	void shouldTokenizeAlphanumeric() {
		assertThat(Tokenizer.alphanumeric().tokenize("spring.datasource.url").toList())
				.containsExactly("spring", "datasource", "url");
	}

	@Test
	@DisplayName("should split on a delimiter pattern given as a string")
	void shouldTokenizeOnPatternString() {
		assertThat(Tokenizer.pattern("[^a-zA-Z0-9]+").tokenize("spring.datasource.url").toList())
				.containsExactly("spring", "datasource", "url");
	}

	@Test
	@DisplayName("should throw for an invalid pattern syntax")
	void shouldThrowForInvalidPatternSyntax() {
		assertThatExceptionOfType(PatternSyntaxException.class)
				.isThrownBy(() -> Tokenizer.pattern("[unterminated"));
	}

	@Test
	@DisplayName("should split on a compiled delimiter pattern")
	void shouldTokenizeOnCompiledPattern() {
		final Pattern pattern = Pattern.compile("[^a-zA-Z0-9]+");

		assertThat(Tokenizer.pattern(pattern).tokenize("foo-bar_baz").toList())
				.containsExactly("foo", "bar", "baz");
	}

	@Test
	@DisplayName("should apply a positive split limit as per Pattern#split(CharSequence, int)")
	void shouldTokenizeWithPositiveLimit() {
		assertThat(Tokenizer.pattern(Pattern.compile(","), 2).tokenize("a,b,c").toList())
				.containsExactly("a", "b,c");
	}

	@Test
	@DisplayName("should discard trailing empty tokens with a zero limit")
	void shouldDiscardTrailingEmptyTokensWithZeroLimit() {
		assertThat(Tokenizer.pattern(Pattern.compile(","), 0).tokenize("a,b,,").toList())
				.containsExactly("a", "b");
	}

	@Test
	@DisplayName("should keep trailing empty tokens with a negative limit")
	void shouldKeepTrailingEmptyTokensWithNegativeLimit() {
		assertThat(Tokenizer.pattern(Pattern.compile(","), -1).tokenize("a,b,,").toList())
				.containsExactly("a", "b", "", "");
	}

}
