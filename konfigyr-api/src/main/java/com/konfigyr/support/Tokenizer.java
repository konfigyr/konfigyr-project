package com.konfigyr.support;

import org.jspecify.annotations.NullMarked;

import java.util.regex.Pattern;

/**
 * Splits a value into an ordered sequence of {@link Tokens}.
 * <p>
 * Implementations decide how a value is split, e.g., by whitespace, punctuation, pattern, or n-grams,
 * but are only responsible for splitting. What the resulting tokens are used for is entirely up to
 * the caller.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@FunctionalInterface
public interface Tokenizer {

	/**
	 * Returns a {@link Tokenizer} that treats the entire input as a single token, performing no
	 * splitting at all.
	 *
	 * @return a {@link Tokenizer} that yields the input unchanged as its only token
	 */
	static Tokenizer identity() {
		return value -> new Tokens(new String[] {value});
	}

	/**
	 * Returns a {@link Tokenizer} that splits a value on runs of whitespace.
	 * <p>
	 * Leading and trailing whitespace is ignored, so a value consisting entirely of whitespace
	 * (or the empty string) produces a single empty token, use {@link Tokens#filter(java.util.function.Predicate)}
	 * afterward if blank tokens should be discarded.
	 *
	 * @return a {@link Tokenizer} that splits on whitespace
	 */
	static Tokenizer whitespace() {
		return value -> new Tokens(Patterns.WHITESPACE.split(value.strip()));
	}

	/**
	 * Returns a {@link Tokenizer} that splits a value into its alphanumeric runs, treating any run of
	 * non-alphanumeric characters (e.g. {@code .}, {@code -}, {@code _}) as a delimiter, so a dotted or
	 * hyphenated identifier like {@code spring.datasource.url} tokenizes as {@code ["spring", "datasource", "url"]}.
	 * <p>
	 * This mirrors the Spring Configuration property name normalization performed by the search vector triggers
	 * ({@code regexp_replace(name, '[^a-zA-Z0-9]+', ' ', 'g')}). If this pattern is ever changed (e.g., to a
	 * Unicode-aware {@code [^\p{L}\p{N}]+}), those triggers must be updated to match, or indexed search terms
	 * and query terms will tokenize differently.
	 *
	 * @return a {@link Tokenizer} that splits on runs of non-alphanumeric characters
	 */
	static Tokenizer alphanumeric() {
		return pattern(Patterns.ALPHANUMERIC);
	}

	/**
	 * Returns a {@link Tokenizer} that splits a value using the given regular expression as a delimiter,
	 * as if by {@link Pattern#compile(String)} followed by {@link Pattern#split(CharSequence)}.
	 * <p>
	 * The expression is treated as a delimiter to split <em>on</em>, not as a pattern that tokens must
	 * match, e.g. {@code "[^a-zA-Z0-9]+"} splits on runs of non-alphanumeric characters, yielding the
	 * alphanumeric runs in between as tokens.
	 *
	 * @param pattern the regular expression to compile and split on, can't be {@code null}
	 * @return a {@link Tokenizer} backed by the compiled pattern, never {@code null}
	 * @throws java.util.regex.PatternSyntaxException if {@code pattern}'s syntax is invalid
	 * @see Pattern#compile(String)
	 */
	static Tokenizer pattern(String pattern) {
		return pattern(Pattern.compile(pattern));
	}

	/**
	 * Returns a {@link Tokenizer} that splits a value using the given {@link Pattern} as a delimiter,
	 * as if by {@link Pattern#split(CharSequence)}.
	 * <p>
	 * The pattern is treated as a delimiter to split <em>on</em>, not as a pattern that tokens must
	 * match, e.g. {@code Pattern.compile("[^a-zA-Z0-9]+")} splits on runs of non-alphanumeric characters,
	 * yielding the alphanumeric runs in between as tokens.
	 *
	 * @param pattern the delimiter pattern to split on, can't be {@code null}
	 * @return a {@link Tokenizer} backed by the given pattern, never {@code null}
	 * @see Pattern#split(CharSequence)
	 */
	static Tokenizer pattern(Pattern pattern) {
		return pattern(pattern, 0);
	}

	/**
	 * Returns a {@link Tokenizer} that splits a value using the given {@link Pattern} as a delimiter,
	 * as if by {@link Pattern#split(CharSequence, int)}.
	 * <p>
	 * The pattern is treated as a delimiter to split <em>on</em>, not as a pattern that tokens must match.
	 * The {@code limit} parameter controls the resulting array as per {@link Pattern#split(CharSequence, int)}: a
	 * positive value applies the pattern at most {@code limit - 1} times and keeps any trailing content
	 * (including trailing empty strings) as the final token; zero applies the pattern as many times as
	 * possible and discards trailing empty strings; a negative value applies the pattern as many times
	 * as possible and keeps trailing empty strings.
	 *
	 * @param pattern the delimiter pattern to split on, can't be {@code null}
	 * @param limit   the result-limit/behavior control, per {@link Pattern#split(CharSequence, int)}
	 * @return a {@link Tokenizer} backed by the given pattern and limit, never {@code null}
	 * @see Pattern#split(CharSequence, int)
	 */
	static Tokenizer pattern(Pattern pattern, int limit) {
		return input -> new Tokens(pattern.split(input, limit));
	}

	/**
	 * Splits the given value into tokens.
	 *
	 * @param value the value to split; may be empty but must not be {@code null}
	 * @return the resulting {@link Tokens}, in the order produced by this
	 *         tokenizer; never {@code null}, may be empty
	 */
	Tokens tokenize(String value);

	final class Patterns {
		private static final Pattern WHITESPACE = Pattern.compile("\\s+");
		private static final Pattern ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9]+");
	}

}
