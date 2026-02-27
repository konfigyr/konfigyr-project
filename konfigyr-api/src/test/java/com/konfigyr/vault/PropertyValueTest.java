package com.konfigyr.vault;

import com.google.crypto.tink.subtle.Hex;
import com.konfigyr.crypto.CryptoException;
import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.test.TestKeysetOperations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyValueTest {

	@Spy
	static KeysetOperations operations = TestKeysetOperations.create();

	@Test
	@DisplayName("should create a new unsealed property value for profile")
	void generateValue() {
		final var value = PropertyValue.create(EntityId.from(1L), "spring.application.name", "test");

		assertThat(value)
				.returns(true, PropertyValue::isUnsealed)
				.returns(false, PropertyValue::isSealed)
				.returns(ByteArray.fromString("test"), PropertyValue::get);

		assertThat(value.checksum())
				.extracting(ByteArray::array)
				.extracting(Hex::encode)
				.isEqualTo("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");

		assertThat(value.toString())
				.isEqualTo("Unsealed(9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08)");
	}

	@ValueSource(strings = {
			"spring.main.banner-mode",
			"spring.main.bannerMode",
			"spring.main.bannermode",
			"Spring.main.banner-mode",
			"spring.Main.banner-mode",
			"spring.main.Banner-Mode",
			"spring.main.BannerMode",
			"spring.main.Bannermode"
	})
	@ParameterizedTest(name = "configuration property name: {0}")
	@DisplayName("should use the Spring configuration name hash code to generate unique checksums")
	void generateChecksumForName(String name) {
		final var value = PropertyValue.create(EntityId.from(1L), name, "off");

		assertThat(value.checksum())
				.extracting(ByteArray::array)
				.extracting(Hex::encode)
				.isEqualTo("b4dc66dde806261bdda8607d8707aa727d308cd80272381a5583f63899918467");
	}

	@ValueSource(strings = { "", "  ", "\n", "\r\n" })
	@ParameterizedTest(name = "property name: \"{0}\"")
	@DisplayName("should fail to create a property value for invalid property name")
	void generateValueFromInvalidPropertyName(String name) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> PropertyValue.create(EntityId.from(1L), name, "off"))
				.withMessageContaining("Invalid configuration property name: %s", name);
	}

	@Test
	@DisplayName("should be able to unseal sealed values and sealed unsealed values")
	void performKeysetOperations() {
		final var unsealed = PropertyValue.create(EntityId.from(99L), "spring.application.name", "sealing");
		final var sealed = unsealed.seal(operations);

		assertThat(sealed)
				.returns(true, PropertyValue::isSealed)
				.returns(false, PropertyValue::isUnsealed);

		assertThat(sealed.get())
				.as("Sealed value should not be equal to unsealed value")
				.isNotEqualTo(unsealed.get());

		assertThat(sealed.checksum())
				.as("Sealed checksum should be equal to unsealed checksum")
				.isEqualTo(unsealed.checksum());

		assertThat(sealed.unseal(operations))
				.isEqualTo(unsealed)
				.returns(unsealed.get(), PropertyValue::get)
				.returns(unsealed.checksum(), PropertyValue::checksum);

		verify(operations).encrypt(unsealed.get(), unsealed.checksum());
		verify(operations).decrypt(sealed.get(), sealed.checksum());
	}

	@Test
	@DisplayName("should not perform any sealing operation on sealed values")
	void sealingAlreadySealedValues() {
		final var sealed = PropertyValue.sealed(ByteArray.fromString("value"), ByteArray.fromString("checksum"));

		assertThat(sealed.seal(operations))
				.isSameAs(sealed);

		verifyNoInteractions(operations);
	}

	@Test
	@DisplayName("should not perform any unsealing operation on unsealed values")
	void unsealingAlreadyUnsealedValues() {
		final var unsealed = PropertyValue.unsealed(ByteArray.fromString("value"), ByteArray.fromString("checksum"));

		assertThat(unsealed.unseal(operations))
				.isSameAs(unsealed);

		verifyNoInteractions(operations);
	}

	@Test
	@DisplayName("should rethrow sealing operation exceptions")
	void rethrowSealingExceptions() {
		final var unsealed = PropertyValue.unsealed(ByteArray.fromString("value"), ByteArray.fromString("checksum"));

		doThrow(CryptoException.KeysetOperationException.class).when(operations)
				.encrypt(unsealed.get(), unsealed.checksum());

		assertThatExceptionOfType(CryptoException.class)
				.isThrownBy(() -> unsealed.seal(operations));
	}

	@Test
	@DisplayName("should rethrow unsealing operation exceptions")
	void rethrowUnsealingExceptions() {
		final var sealed = PropertyValue.sealed(ByteArray.fromString("value"), ByteArray.fromString("checksum"));

		doThrow(CryptoException.KeysetOperationException.class).when(operations)
				.decrypt(sealed.get(), sealed.checksum());

		assertThatExceptionOfType(CryptoException.class)
				.isThrownBy(() -> sealed.unseal(operations));
	}

	@Test
	@DisplayName("should check property value equality by comparing the value type and checksum")
	void assertEquality() {
		final var value = PropertyValue.sealed(ByteArray.fromString("value"), ByteArray.fromString("checksum"));

		assertThat(value)
				.as("should be equal to sealed value with the same checksum and value")
				.isEqualTo(PropertyValue.sealed(value.get(), value.checksum()));

		assertThat(value)
				.as("should be equal to sealed value with the same checksum but different value")
				.isEqualTo(PropertyValue.sealed(ByteArray.fromString("other"), value.checksum()));

		assertThat(value)
				.as("should not be equal to unsealed value with the same checksum and value")
				.isNotEqualTo(PropertyValue.unsealed(value.get(), value.checksum()));

		assertThat(value)
				.as("should not be equal to unsealed value with the same checksum but different value")
				.isNotEqualTo(PropertyValue.unsealed(ByteArray.fromString("other"), value.checksum()));
	}

	@Test
	@DisplayName("should check property value identity by comparing the value type and checksum")
	void assertIdentity() {
		final var value = PropertyValue.sealed(ByteArray.fromString("value"), ByteArray.fromString("checksum"));

		assertThat(value)
				.as("should have same hash code to sealed value with the same checksum and value")
				.hasSameHashCodeAs(PropertyValue.sealed(value.get(), value.checksum()));

		assertThat(value)
				.as("should be same hash code to sealed value with the same checksum but different value")
				.hasSameHashCodeAs(PropertyValue.sealed(ByteArray.fromString("other"), value.checksum()));

		assertThat(value)
				.as("should not have the same hash code to unsealed value with the same checksum and value")
				.doesNotHaveSameHashCodeAs(PropertyValue.unsealed(value.get(), value.checksum()));

		assertThat(value)
				.as("should not have the same hash code to unsealed value with the same checksum but different value")
				.doesNotHaveSameHashCodeAs(PropertyValue.unsealed(ByteArray.fromString("other"), value.checksum()));
	}

}
