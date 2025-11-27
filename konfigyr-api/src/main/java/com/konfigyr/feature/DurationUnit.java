package com.konfigyr.feature;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Defines the duration unit for time based feature policies. It can be used for:
 * <ul>
 *     <li>- API call limits</li>
 *     <li>- Email sends per day</li>
 *     <li>- Login attempts per hour</li>
 *     <li>- Message or file uploads</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@RequiredArgsConstructor
public enum DurationUnit {

	/**
	 * The duration unit that represents a second time unit.
	 * <p>
	 * - Suitable for very high-throughput systems (e.g., API request throttling).<br>
	 * - Often used to enforce "N requests per second" policies.<br>
	 * - Example: 10 messages per second per user.
	 */
	SECONDS('s', TimeUnit.SECONDS),

	/**
	 * The duration unit that represents a minute time unit.
	 * <p>
	 * - Useful for moderate-frequency operations like login attempts.<br>
	 * - Often easier to enforce with a background scheduler or sliding window algorithm.<br>
	 * - Example: 100 requests per minute per IP address.
	 */
	MINUTES('m', TimeUnit.MINUTES),

	/**
	 * The duration unit that represents an hour time unit.
	 * <p>
	 * - Common for operational metrics and usage tracking.<br>
	 * - Suitable for use cases like "500 emails per hour" or "30 uploads per hour".<br>
	 * - Less sensitive than per-second limits, reduces infrastructure strain.
	 */
	HOURS('h', TimeUnit.HOURS),

	/**
	 * The duration unit that represents a day time unit.
	 * <p>
	 * - Ideal for subscription-based quotas or account-wide usage caps.<br>
	 * - Examples: "Send up to 10,000 emails per day", "Download limit of 5GB/day".
	 */
	DAYS('d', TimeUnit.DAYS);

	private final char symbol;
	private final TimeUnit value;

	/**
	 * Character symbol that uniquely identifies this {@link DurationUnit}. For instance a symbol of {@code s}
	 * identifies a {@link DurationUnit#SECONDS} duration unit.
	 *
	 * @return the duration unit symbol
	 */
	public char symbol() {
		return symbol;
	}

	/**
	 * Converts this {@link DurationUnit} to the equivalent {@link TimeUnit}.
	 *
	 * @return the duration time unit value, never {@literal null}
	 */
	@NonNull
	public TimeUnit toTimeUnit() {
		return value;
	}

	/**
	 * Converts this {@link DurationUnit} to the equivalent {@link ChronoUnit}.
	 *
	 * @return the duration chrono unit value, never {@literal null}
	 */
	@NonNull
	public ChronoUnit toChronoUnit() {
		return value.toChronoUnit();
	}

	/**
	 * Attempts to resolve the {@link DurationUnit} from the given character sequence. When the given
	 * sequence is {@literal null} or it does not match any duration unit symbol, an {@link IllegalArgumentException}
	 * would be thrown.
	 *
	 * @param value character sequence that should match to a duration unit symbol
	 * @return duration unit matching the sequence, never {@literal null}
	 * @throws IllegalArgumentException when no {@link DurationUnit} symbol matches the character sequence
	 */
	@NonNull
	static DurationUnit from(CharSequence value) {
		if (value == null || value.length() != 1) {
			throw new IllegalArgumentException("Invalid duration unit symbol: " + value);
		}

		return from(value.charAt(0));
	}

	/**
	 * Attempts to resolve the {@link DurationUnit} from the given character. When the given character does
	 * not match any duration unit symbol, an {@link IllegalArgumentException} would be thrown.
	 *
	 * @param value character that should match to a duration unit symbol
	 * @return duration unit matching the character, never {@literal null}
	 * @throws IllegalArgumentException when no {@link DurationUnit} symbol matches the character
	 */
	@NonNull
	static DurationUnit from(char value) {
		for (DurationUnit unit : DurationUnit.values()) {
			if (unit.symbol == value) {
				return unit;
			}
		}
		throw new IllegalArgumentException("Invalid duration unit symbol: " + value);
	}

	/**
	 * Attempts to resolve the {@link DurationUnit} from the {@link TimeUnit}. When the given time unit
	 * not match any duration unit, an {@link IllegalArgumentException} would be thrown.
	 *
	 * @param value time unit for which duration unit would be resolved
	 * @return duration unit matching the time unit, never {@literal null}
	 * @throws IllegalArgumentException when no {@link DurationUnit} supports the time unit
	 */
	@NonNull
	static DurationUnit from(@Nullable TimeUnit value) {
		Assert.notNull(value, () -> "Invalid duration unit: " + value);

		return switch (value) {
			case SECONDS -> DurationUnit.SECONDS;
			case MINUTES -> DurationUnit.MINUTES;
			case HOURS -> DurationUnit.HOURS;
			case DAYS -> DurationUnit.DAYS;
			default -> throw new IllegalArgumentException("Invalid duration unit: " + value);
		};
	}

	/**
	 * Attempts to resolve the {@link DurationUnit} from the {@link ChronoUnit}. When the given time unit
	 * not match any duration unit, an {@link IllegalArgumentException} would be thrown.
	 *
	 * @param value time unit for which duration unit would be resolved
	 * @return duration unit matching the time unit, never {@literal null}
	 * @throws IllegalArgumentException when no {@link DurationUnit} supports the time unit
	 */
	@NonNull
	static DurationUnit from(@Nullable ChronoUnit value) {
		Assert.notNull(value, () -> "Invalid duration unit: " + value);

		return switch (value) {
			case SECONDS -> DurationUnit.SECONDS;
			case MINUTES -> DurationUnit.MINUTES;
			case HOURS -> DurationUnit.HOURS;
			case DAYS -> DurationUnit.DAYS;
			default -> throw new IllegalArgumentException("Invalid duration unit: " + value);
		};
	}
}
