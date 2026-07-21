package com.konfigyr.support;

import lombok.EqualsAndHashCode;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.convert.converter.Converter;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An immutable, ordered collection of tokens produced by a {@link Tokenizer}.
 * <p>
 * This type carries tokens between a {@link Tokenizer} and whatever consumes them; it makes
 * no assumptions about the tokens' content or their intended use.
 * <p>
 * Instances of this class are immutable and safe to share and reuse.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see Tokenizer
 */
@NullMarked
@EqualsAndHashCode
public final class Tokens implements Iterable<String> {

	private static final Tokens EMPTY = new Tokens(List.of());

	private final List<String> values;

	/**
	 * Creates a new {@link Tokens} from the given values.
	 *
	 * @param values the token values, in order; none may be {@code null}
	 */
	Tokens(String[] values) {
		this(List.of(values));
	}

	private Tokens(List<String> values) {
		this.values = values;
	}

	/**
	 * Returns an empty {@link Tokens}.
	 *
	 * @return an empty {@link Tokens} instance
	 */
	public static Tokens empty() {
		return EMPTY;
	}

	/**
	 * Returns whether this instance contains no tokens.
	 *
	 * @return {@code true} if there are no tokens, {@code false} otherwise
	 */
	public boolean isEmpty() {
		return values.isEmpty();
	}

	/**
	 * Returns the number of tokens.
	 *
	 * @return the token count
	 */
	public int size() {
		return values.size();
	}

	/**
	 * Returns a sequential stream over the tokens, in their original order.
	 *
	 * @return a {@link Stream} of tokens; never {@code null}
	 */
	public Stream<String> stream() {
		return values.stream();
	}

	/**
	 * Returns a new {@link Tokens} containing only the tokens that satisfy the given predicate,
	 * preserving their original order.
	 *
	 * @param predicate the condition a token must satisfy to be kept
	 * @return a new {@link Tokens} with non-matching tokens removed, never {@code null}
	 */
	public Tokens filter(Predicate<String> predicate) {
		return new Tokens(stream().filter(predicate).toList());
	}

	/**
	 * Returns a new {@link Tokens} with each token transformed by the given function, preserving
	 * order and count.
	 *
	 * @param mapper the per-token transformation; must not return {@code null} for any input
	 * @return a new {@link Tokens} with each token replaced by the result of {@code mapper}, never {@code null}
	 */
	public Tokens map(UnaryOperator<String> mapper) {
		return new Tokens(stream().map(mapper).toList());
	}

	/**
	 * Returns an immutable {@link List} view of the tokens, in their
	 * original order.
	 *
	 * @return a {@link List} of tokens; never {@code null}
	 */
	public List<String> toList() {
		return values;
	}

	/**
	 * Joins the tokens into a single {@link String}, in their original order, separated by {@code delimiter}.
	 * <p>
	 * Joining zero tokens yields an empty string, same as {@link String#join(CharSequence, CharSequence...)}
	 * and {@link Collectors#joining(CharSequence)}. This method makes no assumption about whether an empty
	 * result is meaningful to the caller; that judgment belongs to whatever consumes the result.
	 *
	 * @param delimiter the separator placed between each token, can't be {@code null}
	 * @return the joined string, never {@code null}
	 */
	public String join(CharSequence delimiter) {
		return stream().collect(Collectors.joining(delimiter));
	}

	/**
	 * Joins the tokens into a single {@link String}, in their original order, separated by {@code delimiter}
	 * and wrapped with {@code prefix} and {@code suffix}.
	 * <p>
	 * Joining zero tokens yields {@code prefix + suffix}, same as
	 * {@link Collectors#joining(CharSequence, CharSequence, CharSequence)}.
	 *
	 * @param delimiter the separator placed between each token, can't be {@code null}
	 * @param prefix the text prepended to the result, can't be {@code null}
	 * @param suffix the text appended to the result, can't be {@code null}
	 * @return the joined string, never {@code null}
	 */
	public String join(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
		return stream().collect(Collectors.joining(delimiter, prefix, suffix));
	}

	/**
	 * Applies {@code transformer} to this {@link Tokens} and returns its result, letting a fluent
	 * {@code filter}/{@code map} pipeline finish with a conversion to some other type in a single
	 * expression, rather than breaking out into a separate statement.
	 *
	 * @param transformer the conversion applied to this {@link Tokens}, can't be {@code null}
	 * @param <T> the type produced by {@code transformer}
	 * @return the result of applying {@code transformer} to this {@link Tokens}
	 */
	public <T> T transform(Converter<Tokens, T> transformer) {
		return transformer.convert(this);
	}

	@Override
	public Iterator<String> iterator() {
		return values.iterator();
	}

	@Override
	public String toString() {
		return join(", ", "Tokens(", ")");
	}
}
