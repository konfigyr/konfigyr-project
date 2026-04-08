package com.konfigyr.vault.history;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class HistoryCursorTokenTest {

	@ValueSource(strings = {
			"", "  ", "invalid", "AAABnUlDfTxkN2FlYWVlZTVhNmE1", "36136243474196827672847",
			"AAABn312UlDfTxkN2FlYWVlZTVhNmE172847", "AAABnUlDfTxkN2FlYWVlZTVhNmE1ZTIwNGQ4Zj"
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
				124097687512745L,
				OffsetDateTime.parse("2026-04-01T13:37:32.988Z")
		);

		assertThat(token)
				.returns(124097687512745L, HistoryCursorToken::identifier)
				.returns(OffsetDateTime.parse("2026-04-01T13:37:32.988Z"), HistoryCursorToken::timestamp)
				.returns("0100000070ddbe94e6a90000019d49437d3c", HistoryCursorToken::value);

		assertThat(HistoryCursorToken.decode(token.value()))
				.isEqualTo(token);
	}

	@Test
	@DisplayName("should encode and decode reversed cursor pagination tokens")
	void shouldEncodeAndDecodeReverseTokens() {
		final var token = HistoryCursorToken.of(
				9124582095215679363L,
				OffsetDateTime.parse("2026-04-07T12:15:11.352Z"),
				true
		);

		assertThat(token)
				.returns(9124582095215679363L, HistoryCursorToken::identifier)
				.returns(OffsetDateTime.parse("2026-04-07T12:15:11.352Z"), HistoryCursorToken::timestamp)
				.returns("01017ea107124d3857830000019d67de3df8", HistoryCursorToken::value);

		assertThat(HistoryCursorToken.decode(token.value()))
				.isEqualTo(token);
	}

}
