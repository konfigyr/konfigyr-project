package com.konfigyr.security;

import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.io.ByteArrayCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

class NamespaceClientIdTest {

	final EntityId namespaceId = EntityId.from(42L);

	@EnumSource(NamespaceClientType.class)
	@ParameterizedTest(name = "generate client_id for type: {0}")
	@DisplayName("should generate a client ID that round-trips back to the same namespace ID and type")
	void roundTrip(NamespaceClientType type) {
		final var id = NamespaceClientId.of(namespaceId, type);

		assertThat(id.get())
				.startsWith(NamespaceClientId.PREFIX)
				.hasSize(47);

		final var parsed = NamespaceClientId.parse(id.get());

		assertThat(parsed.namespace())
				.isEqualTo(id.namespace())
				.isEqualTo(namespaceId);

		assertThat(parsed.type())
				.isEqualTo(id.type())
				.isEqualTo(type);

		assertThat(parsed.timestamp())
				.isEqualTo(id.timestamp())
				.isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));

		assertThat(parsed.bytes())
				.isEqualTo(id.bytes())
				.returns(NamespaceClientId.BUFFER_SIZE, ByteArray::size);
	}

	@Test
	@DisplayName("should encode 32 bytes as a 43-character base64url string after the prefix")
	void encodedLength() {
		final var id = NamespaceClientId.of(namespaceId, NamespaceClientType.AGENT);

		// 32 bytes base64url without padding: ceil(32 * 4 / 3) = 43 chars
		assertThat(id.get().substring(NamespaceClientId.PREFIX.length()))
				.hasSize(43);
	}

	@Test
	@DisplayName("should produce different values for two calls with the same namespace ID and type")
	void randomnessMakesValuesUnique() {
		assertThat(NamespaceClientId.of(namespaceId, NamespaceClientType.SERVICE_ACCOUNT).get())
				.isNotEqualTo(NamespaceClientId.of(namespaceId, NamespaceClientType.SERVICE_ACCOUNT).get())
				.isNotEqualTo(NamespaceClientId.of(namespaceId, NamespaceClientType.SERVICE_ACCOUNT).get());
	}

	@Test
	@DisplayName("isPotentialClientId should match the prefix without full validation")
	void isPotentialClientId() {
		assertThat(NamespaceClientId.isPotentialClientId(null)).isFalse();
		assertThat(NamespaceClientId.isPotentialClientId("")).isFalse();
		assertThat(NamespaceClientId.isPotentialClientId("konfigyr")).isFalse();
		assertThat(NamespaceClientId.isPotentialClientId("kfg-tooshort")).isTrue();
		assertThat(NamespaceClientId.isPotentialClientId(
				NamespaceClientId.of(namespaceId, NamespaceClientType.PIPELINE).get())).isTrue();
	}

	@Test
	@DisplayName("tryParse should return empty for null and arbitrary strings")
	void tryParseReturnEmptyForInvalidInput() {
		assertThat(NamespaceClientId.tryParse(null)).isEmpty();
		assertThat(NamespaceClientId.tryParse("")).isEmpty();
		assertThat(NamespaceClientId.tryParse("not-a-client-id")).isEmpty();
		assertThat(NamespaceClientId.tryParse("konfigyr")).isEmpty();
	}

	@Test
	@DisplayName("tryParse should return empty for strings with the correct prefix but wrong payload")
	void tryParseReturnEmptyForWrongPrefix() {
		assertThat(NamespaceClientId.tryParse("kfg-tooshort")).isEmpty();
		assertThat(NamespaceClientId.tryParse("kfg-!!!notbase64!!!")).isEmpty();
	}

	@Test
	@DisplayName("parse should throw on blank input")
	void parseThrowsOnBlank() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> NamespaceClientId.parse(""));

		assertThatIllegalArgumentException()
				.isThrownBy(() -> NamespaceClientId.parse("   "));
	}

	@Test
	@DisplayName("parse should throw on missing prefix")
	void parseThrowsOnMissingPrefix() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> NamespaceClientId.parse("notvalid"))
				.withMessageContaining("prefix");
	}

	@Test
	@DisplayName("parse should throw on wrong payload length")
	void parseThrowsOnWrongLength() {
		// 24-byte payload (old format without type byte) encodes to 32 chars; now invalid
		assertThatIllegalArgumentException()
				.isThrownBy(() -> NamespaceClientId.parse("kfg-A2c7mvoxEP346BQCSuwnJ5ZNQIEsgCBG"))
				.withMessageContaining("length");
	}

	@Test
	@DisplayName("parse should throw on unsupported version byte")
	void parseThrowsOnUnsupportedVersion() {
		final byte[] payload = new byte[NamespaceClientId.BUFFER_SIZE];
		payload[0] = (byte) 0xFF; // unknown version

		final String encoded = NamespaceClientId.PREFIX
				+ ByteArray.from(payload, 0, payload.length).encode(ByteArrayCodec.BASE64_URL_SAFE_NO_PADDING);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> NamespaceClientId.parse(encoded))
				.withMessageContaining("version");
	}

	@Test
	@DisplayName("parse should throw on unsupported type byte")
	void parseThrowsOnUnsupportedType() {
		final byte[] payload = new byte[NamespaceClientId.BUFFER_SIZE];
		payload[0] = NamespaceClientId.VERSION; // valid version
		payload[1] = (byte) 0xFF;               // unknown type

		final String encoded = NamespaceClientId.PREFIX
				+ ByteArray.from(payload, 0, payload.length).encode(ByteArrayCodec.BASE64_URL_SAFE_NO_PADDING);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> NamespaceClientId.parse(encoded))
				.withMessageContaining("type");
	}

	@Test
	@DisplayName("equality is based on value string only")
	void equalityOnValue() {
		final var id = NamespaceClientId.of(namespaceId, NamespaceClientType.AGENT);
		final var reparsed = NamespaceClientId.parse(id.get());

		assertThat(id).isEqualTo(reparsed);
		assertThat(id).hasSameHashCodeAs(reparsed);
	}

	@Test
	@DisplayName("toString delegates to get()")
	void toStringDelegatesToGet() {
		final var id = NamespaceClientId.of(namespaceId, NamespaceClientType.SERVICE_ACCOUNT);

		assertThat(id.toString()).isEqualTo(id.get());
	}

	@Test
	@DisplayName("should be compatible with Jackson")
	void jacksonCompatibility() {
		final var id = NamespaceClientId.of(namespaceId, NamespaceClientType.PIPELINE);
		final var mapper = JsonMapper.shared();

		final var json = mapper.writeValueAsString(id);

		assertThat(json)
				.isEqualTo("\"%s\"", id.get());

		assertThat(mapper.readValue(json, NamespaceClientId.class))
				.isEqualTo(id);
	}
}
