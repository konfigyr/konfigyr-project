package com.konfigyr.feature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class DurationUnitTest {

	@ValueSource(strings = { "", "  ", "y", "ms", "invalid" })
	@DisplayName("should convert symbol to duration unit")
	@ParameterizedTest(name = "fail to convert symbol \"{0}\" to unit")
	void invalidSymbols(String symbol) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> DurationUnit.from(symbol))
				.withMessageContaining("Invalid duration unit symbol: %s", symbol);
	}

	@MethodSource("symbols")
	@DisplayName("should convert symbol to duration unit")
	@ParameterizedTest(name = "convert symbol \"{0}\" to unit: {1}")
	void convertSymbolToUnit(String symbol, DurationUnit expected) {
		assertThat(DurationUnit.from(symbol))
				.isEqualTo(expected);
	}

	@MethodSource("symbols")
	@DisplayName("should convert duration unit to symbol")
	@ParameterizedTest(name = "convert unit {1} to symbol: \"{0}\"")
	void convertUnitToSymbol(char expected, DurationUnit unit) {
		assertThat(unit.symbol())
				.isEqualTo(expected);
	}

	@MethodSource("chronos")
	@DisplayName("should convert chrono unit to duration unit")
	@ParameterizedTest(name = "convert chrono unit {0} to unit: {1}")
	void convertChronoUnitToDurationUnit(ChronoUnit unit, DurationUnit expected) {
		assertThat(DurationUnit.from(unit))
				.isEqualTo(expected);
	}

	@MethodSource("chronos")
	@DisplayName("should convert duration unit to chrono unit")
	@ParameterizedTest(name = "convert duration unit {1} to chrono unit: {0}")
	void convertDurationUnitToChronoUnit(ChronoUnit expected, DurationUnit unit) {
		assertThat(unit.toChronoUnit())
				.isEqualTo(expected);
	}

	@Test
	@DisplayName("should fail to convert duration unit from unsupported chrono unit")
	void unsupportedChronoUnit() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> DurationUnit.from(ChronoUnit.HALF_DAYS))
				.withMessageContaining("Invalid duration unit: %s", ChronoUnit.HALF_DAYS);
	}

	@MethodSource("times")
	@DisplayName("should convert chrono unit to duration unit")
	@ParameterizedTest(name = "convert chrono unit {0} to unit: {1}")
	void convertTimeUnitToDurationUnit(TimeUnit unit, DurationUnit expected) {
		assertThat(DurationUnit.from(unit))
				.isEqualTo(expected);
	}

	@MethodSource("times")
	@DisplayName("should convert duration unit to time unit")
	@ParameterizedTest(name = "convert duration unit {1} to time unit: {0}")
	void convertDurationUnitToTimeUnit(TimeUnit expected, DurationUnit unit) {
		assertThat(unit.toTimeUnit())
				.isEqualTo(expected);
	}

	@Test
	@DisplayName("should fail to convert duration unit from unsupported time unit")
	void unsupportedTimeUnit() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> DurationUnit.from(TimeUnit.MICROSECONDS))
				.withMessageContaining("Invalid duration unit: %s", TimeUnit.MICROSECONDS);
	}

	static Stream<Arguments> symbols() {
		return Stream.of(
				Arguments.of("s", DurationUnit.SECONDS),
				Arguments.of("m", DurationUnit.MINUTES),
				Arguments.of("h", DurationUnit.HOURS),
				Arguments.of("d", DurationUnit.DAYS)
		);
	}

	static Stream<Arguments> chronos() {
		return Stream.of(
				Arguments.of(ChronoUnit.SECONDS, DurationUnit.SECONDS),
				Arguments.of(ChronoUnit.MINUTES, DurationUnit.MINUTES),
				Arguments.of(ChronoUnit.HOURS, DurationUnit.HOURS),
				Arguments.of(ChronoUnit.DAYS, DurationUnit.DAYS)
		);
	}

	static Stream<Arguments> times() {
		return Stream.of(
				Arguments.of(TimeUnit.SECONDS, DurationUnit.SECONDS),
				Arguments.of(TimeUnit.MINUTES, DurationUnit.MINUTES),
				Arguments.of(TimeUnit.HOURS, DurationUnit.HOURS),
				Arguments.of(TimeUnit.DAYS, DurationUnit.DAYS)
		);
	}
}
