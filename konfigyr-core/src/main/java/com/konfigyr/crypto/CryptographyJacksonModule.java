package com.konfigyr.crypto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.konfigyr.io.ByteArray;
import tools.jackson.databind.module.SimpleModule;

public final class CryptographyJacksonModule extends SimpleModule {

	public CryptographyJacksonModule() {
		super(CryptographyJacksonModule.class.getSimpleName());
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);
		context.setMixIn(ByteArray.class, ByteArrayMixin.class);
	}

	static abstract class ByteArrayMixin {

		@JsonCreator
		static ByteArray fromBase64String(String value) {
			return ByteArray.fromBase64String(value);
		}

		@JsonValue
		abstract String encodeBase64();

	}
}
