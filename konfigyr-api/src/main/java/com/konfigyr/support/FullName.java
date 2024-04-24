package com.konfigyr.support;

import com.google.errorprone.annotations.Immutable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.function.Supplier;

/**
 * Immutable value objects that represents the persons full name. This class is also capable of parsing name
 * parts out of a single string to create this instance.
 * <p>
 * Please do not expect wonders from this parser as it is a very simple one, it does not detect any salutations,
 * post nominals, middle names, prefixes or suffixes.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Immutable
@ValueObject
@EqualsAndHashCode(of = "value")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FullName implements Supplier<String>, Serializable {

	private final String value;
	private final String firstName;
	private final String lastName;

	/**
	 * Creates a new instance of the full name out of first and name parts.
	 *
	 * @param firstName first name part, can't be blank
	 * @param lastName last name part, can be blank
	 * @return the full name, never {@code null}
	 * @throws IllegalArgumentException when first name is blank
	 */
	@NonNull
	public static FullName of(String firstName, String lastName) {
		Assert.hasText(firstName, "First name can not be blank");

		final StringJoiner joiner = new StringJoiner(" ");
		joiner.add(firstName);

		if (StringUtils.hasText(lastName)) {
			joiner.add(lastName);
		}

		return new FullName(joiner.toString(), firstName, StringUtils.hasText(lastName) ? lastName : "");
	}

	/**
	 * Attempts to parse and consume the value string, that hopefully represents the full name,
	 * and creates the {@link FullName} instance.
	 * <p>
	 * This method would return {@code null} in case value is blank, or it can find any tokens
	 * that could represent the first name.
	 *
	 * @param value value to be parsed, can be {@code null}
	 * @return parsed full name instance or {@code null}
	 */
	@Nullable
	public static FullName parse(String value) {
		if (value == null) {
			return null;
		}

		final StringTokenizer tokenizer = new StringTokenizer(value);
		String firstName;

		if (tokenizer.hasMoreTokens()) {
			firstName = tokenizer.nextToken();
		} else {
			return null;
		}

		final StringBuilder lastName = new StringBuilder();
		while (tokenizer.hasMoreTokens()) {
			lastName.append(tokenizer.nextToken());

			if (tokenizer.hasMoreTokens()) {
				lastName.append(" ");
			}
		}

		return new FullName(value, firstName, lastName.toString());
	}

	/**
	 * Returns the first name part of this full name.
	 *
	 * @return the first name, never {@literal null} or blank
	 */
	@NonNull
	public String firstName() {
		return firstName;
	}

	/**
	 * Returns the last name part of this full name or an empty string if none is defined.
	 *
	 * @return the last name, never {@literal null}
	 */
	@NonNull
	public String lastName() {
		return lastName;
	}

	/**
	 * Returns the full name as a string.
	 *
	 * @return the full name, never {@literal null}
	 */
	@NonNull
	@Override
	public String get() {
		return value;
	}

	@Override
	public String toString() {
		return get();
	}
}
