package com.konfigyr.crypto;

import com.konfigyr.io.ByteArray;


final class Stubs {

	private Stubs() {
		// noop
	}

	static final ByteArray DATA = ByteArray.fromString("operation payload");
	static final ByteArray CONTEXT = ByteArray.fromString("authentication context");
	static final ByteArray RESULT = ByteArray.fromString("operation result");

}
