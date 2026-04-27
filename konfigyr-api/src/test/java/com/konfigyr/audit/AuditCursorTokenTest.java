package com.konfigyr.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AuditCursorTokenTest {

	@ValueSource(strings = {
			"", "  ", "invalid", "AAABnUlDfTxkN2FlYWVlZTVhNmE1", "36136243474196827672847",
			"AAABn312UlDfTxkN2FlYWVlZTVhNmE172847", "AAABnUlDfTxkN2FlYWVlZTVhNmE1ZTIwNGQ4Zj"
	})
	@ParameterizedTest(name = "cursor token = {0}")
	@DisplayName("should fail to decode invalid cursor pagination tokens")
	void invalidCursorPaginationToken(String token) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AuditCursorToken.decode(token))
				.withMessage("Invalid cursor token of: %s", token);
	}

	@Test
	@DisplayName("should encode and decode cursor pagination tokens")
	void shouldEncodeAndDecodeTokens() {
		final var token = AuditCursorToken.of(
				new UUID(124097687512745L, 453456715388L),
				OffsetDateTime.parse("2026-04-01T13:37:32.988Z")
		);

		assertThat(token)
				.returns(UUID.fromString("000070dd-be94-e6a9-0000-006994205e7c"), AuditCursorToken::identifier)
				.returns(OffsetDateTime.parse("2026-04-01T13:37:32.988Z"), AuditCursorToken::timestamp)
				.returns("0100000070ddbe94e6a90000006994205e7c0000019d49437d3c", AuditCursorToken::value);

		assertThat(AuditCursorToken.decode(token.value()))
				.isEqualTo(token);
	}

	@Test
	@DisplayName("should encode and decode reversed cursor pagination tokens")
	void shouldEncodeAndDecodeReverseTokens() {
		final var token = AuditCursorToken.of(
				new UUID(9124582095215679363L, 736510357437L),
				OffsetDateTime.parse("2026-04-07T12:15:11.352Z"),
				true
		);

		assertThat(token)
				.returns(UUID.fromString("7ea10712-4d38-5783-0000-00ab7b702fbd"), AuditCursorToken::identifier)
				.returns(OffsetDateTime.parse("2026-04-07T12:15:11.352Z"), AuditCursorToken::timestamp)
				.returns("01017ea107124d385783000000ab7b702fbd0000019d67de3df8", AuditCursorToken::value);

		assertThat(AuditCursorToken.decode(token.value()))
				.isEqualTo(token);
	}

}
