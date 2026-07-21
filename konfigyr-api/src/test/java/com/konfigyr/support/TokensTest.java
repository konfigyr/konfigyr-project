package com.konfigyr.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class TokensTest {

	@Test
	@DisplayName("should create an empty tokens instance")
	void shouldCreateEmptyTokens() {
		assertThatObject(Tokens.empty())
				.returns(true, Tokens::isEmpty)
				.returns(0, Tokens::size)
				.returns(List.of(), Tokens::toList);
	}

	@Test
	@DisplayName("should create tokens from values, preserving order")
	void shouldCreateTokensFromValues() {
		final Tokens tokens = new Tokens(new String[] { "one", "two", "three" });

		assertThatObject(tokens)
				.returns(false, Tokens::isEmpty)
				.returns(3, Tokens::size)
				.returns(List.of("one", "two", "three"), Tokens::toList);
	}

	@Test
	@DisplayName("should defensively copy the given values array")
	void shouldDefensivelyCopyValues() {
		final String[] values = { "one", "two" };
		final Tokens tokens = new Tokens(values);

		values[0] = "mutated";

		assertThat(tokens.toList()).containsExactly("one", "two");
	}

	@Test
	@DisplayName("should stream tokens in order")
	void shouldStreamTokensInOrder() {
		final Tokens tokens = new Tokens(new String[] { "a", "b", "c" });

		assertThat(tokens.stream()).containsExactly("a", "b", "c");
	}

	@Test
	@DisplayName("should filter tokens, preserving order")
	void shouldFilterTokens() {
		final Tokens tokens = new Tokens(new String[] { "one", "", "two", "", "three" });

		assertThat(tokens.filter(StringUtils::hasText).toList())
				.containsExactly("one", "two", "three");
	}

	@Test
	@DisplayName("should return empty tokens when the filter matches nothing")
	void shouldFilterToEmptyTokens() {
		final Tokens tokens = new Tokens(new String[] { "a", "b" });

		assertThatObject(tokens.filter(value -> false))
				.returns(true, Tokens::isEmpty);
	}

	@Test
	@DisplayName("should map tokens, preserving order and count")
	void shouldMapTokens() {
		final Tokens tokens = new Tokens(new String[] { "one", "two", "three" });

		assertThat(tokens.map(String::toUpperCase).toList())
				.containsExactly("ONE", "TWO", "THREE");
	}

	@Test
	@DisplayName("should iterate tokens in order")
	void shouldIterateTokensInOrder() {
		final Tokens tokens = new Tokens(new String[] { "x", "y", "z" });

		assertThat(tokens).containsExactly("x", "y", "z");
	}

	@Test
	@DisplayName("should render a readable string representation")
	void shouldRenderReadableToString() {
		final Tokens tokens = new Tokens(new String[] { "a", "b" });

		assertThat(tokens.toString()).isEqualTo("Tokens(a, b)");
	}

	@Test
	@DisplayName("should join tokens with the given delimiter")
	void shouldJoinTokensWithDelimiter() {
		final Tokens tokens = new Tokens(new String[] { "one", "two", "three" });

		assertThat(tokens.join(" & ")).isEqualTo("one & two & three");
	}

	@Test
	@DisplayName("should join zero tokens into an empty string")
	void shouldJoinEmptyTokensToEmptyString() {
		assertThat(Tokens.empty().join(" & ")).isEmpty();
	}

	@Test
	@DisplayName("should join a single token with no delimiter applied")
	void shouldJoinSingleToken() {
		final Tokens tokens = new Tokens(new String[] { "one" });

		assertThat(tokens.join(" & ")).isEqualTo("one");
	}

	@Test
	@DisplayName("should join tokens with a delimiter, prefix and suffix")
	void shouldJoinTokensWithDelimiterPrefixAndSuffix() {
		final Tokens tokens = new Tokens(new String[] { "a", "b" });

		assertThat(tokens.join(", ", "[", "]")).isEqualTo("[a, b]");
	}

	@Test
	@DisplayName("should join zero tokens into just the prefix and suffix")
	void shouldJoinEmptyTokensToPrefixAndSuffix() {
		assertThat(Tokens.empty().join(", ", "[", "]")).isEqualTo("[]");
	}

	@Test
	@DisplayName("should apply the transformer to this tokens instance and return its result")
	void shouldTransform() {
		final Tokens tokens = new Tokens(new String[] { "one", "two", "three" });

		final int result = tokens.transform(Tokens::size);

		assertThat(result).isEqualTo(3);
	}

	@Test
	@DisplayName("should pass this tokens instance, unmodified, to the transformer")
	void shouldTransformPassThisInstanceToTransformer() {
		final Tokens tokens = new Tokens(new String[] { "a", "b" });
		final Tokens result = tokens.transform(received -> received);

		assertThat(result).isSameAs(tokens);
	}

	@Test
	@DisplayName("should compose with filter and map to finish a pipeline in a single expression")
	void shouldTransformComposeWithFilterAndMap() {
		final Tokens tokens = new Tokens(new String[] { "one", "", "two" });

		final String result = tokens.filter(StringUtils::hasText)
				.map(word -> word + ":*")
				.transform(words -> words.join(" & "));

		assertThat(result).isEqualTo("one:* & two:*");
	}

}
