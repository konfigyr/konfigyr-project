package com.konfigyr.crypto.shamir;

import com.konfigyr.io.ByteArray;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ShareTest {

	@Test
	@DisplayName("should assert minimum share index value")
	void assertMinIndex() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Share(0, ByteArray.empty()))
				.withMessageContaining("Share index must be greater than 0");
	}

	@Test
	@DisplayName("should assert maximum share index value")
	void assertMaxIndex() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Share(2000, ByteArray.empty()))
				.withMessageContaining("Share index must not be greater than 255");
	}

	@Test
	@DisplayName("should assert that share value is not null")
	void assertNullValue() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Share(1, (ByteArray) null))
				.withMessageContaining("Share byte array value cannot be null");
	}

	@Test
	@DisplayName("should assert that share value is not empty")
	void assertEmptyValue() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Share(1, ByteArray.empty()))
				.withMessageContaining("Share byte array value cannot be empty");
	}

	@Test
	@DisplayName("should assert that encoded share value is not null")
	void assertNullEncodedValue() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Share.from(null))
				.withMessageContaining("Encoded share value cannot be blank");
	}

	@Test
	@DisplayName("should assert that encoded share value is not blank")
	void assertEmptyEncodedValue() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Share.from("  "))
				.withMessageContaining("Encoded share value cannot be blank");
	}

	@Test
	@DisplayName("should create share value from index and data")
	void assertShare() {
		assertThat(new Share(4, "testing".getBytes(StandardCharsets.UTF_8)))
				.returns(4, Share::index)
				.returns(ByteArray.fromString("testing"), Share::value)
				.returns(7, Share::length);
	}

	@Test
	@DisplayName("should encode and decode share value")
	void assertEncoding() {
		final var data = ByteArray.fromString("share value to be encoded");
		final var share = new Share(16, data);

		assertThat(share.get())
				.isNotBlank()
				.isBase64()
				.isPrintable()
				.isEqualTo("AAAAEHNoYXJlIHZhbHVlIHRvIGJlIGVuY29kZWQ");

		assertThat(Share.from(share.get()))
				.isEqualTo(share);
	}

	@Test
	@DisplayName("shares should be comparable by index")
	void assertComparable() {
		final var value = ByteArray.fromString("share value");

		assertThat(List.of(
				new Share(1, value),
				new Share(2, value),
				new Share(3, value),
				new Share(4, value),
				new Share(5, value)
		)).isSorted();
	}

}
