package com.konfigyr.vault.history;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class HistoryCursorTokenTest {

	@ValueSource(strings = {
			"", "  ", "invalid", "AAABnUlDfTxkN2FlYWVlZTVhNmE1", "36136243474196827672847",
			"AAABn312UlDfTxkN2FlYWVlZTVhNmE172847"
	})
	@ParameterizedTest(name = "cursor token = {0}")
	@DisplayName("should fail to decode invalid cursor pagination tokens")
	void invalidCursorPaginationToken(String token) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HistoryCursorToken.decode(token))
				.withMessage("Invalid cursor token of: %s", token);
	}

	@Test
	@DisplayName("should encode and decode cursor pagination tokens")
	void shouldEncodeAndDecodeTokens() {
		final var token = HistoryCursorToken.of(
				UUID.fromString("019690a1-0007-7000-8000-000000000007"),
				OffsetDateTime.parse("2026-04-01T13:37:32.988Z")
		);

		assertThat(token)
				.returns(UUID.fromString("019690a1-0007-7000-8000-000000000007"), HistoryCursorToken::identifier)
				.returns(OffsetDateTime.parse("2026-04-01T13:37:32.988Z"), HistoryCursorToken::timestamp)
				.returns(false, HistoryCursorToken::reversed);

		assertThat(HistoryCursorToken.decode(token.value()))
				.isEqualTo(token);
	}

	@Test
	@DisplayName("should encode and decode reversed cursor pagination tokens")
	void shouldEncodeAndDecodeReverseTokens() {
		final var token = HistoryCursorToken.of(
				UUID.fromString("019690a1-0003-7000-8000-000000000003"),
				OffsetDateTime.parse("2026-04-07T12:15:11.352Z"),
				true
		);

		assertThat(token)
				.returns(UUID.fromString("019690a1-0003-7000-8000-000000000003"), HistoryCursorToken::identifier)
				.returns(OffsetDateTime.parse("2026-04-07T12:15:11.352Z"), HistoryCursorToken::timestamp)
				.returns(true, HistoryCursorToken::reversed);

		assertThat(HistoryCursorToken.decode(token.value()))
				.isEqualTo(token);
	}

}
