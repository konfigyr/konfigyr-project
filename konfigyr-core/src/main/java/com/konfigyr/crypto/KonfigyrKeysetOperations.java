package com.konfigyr.crypto;

import com.konfigyr.io.ByteArray;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.util.function.Supplier;

/**
 * Simple implementation of the {@link KeysetOperations} that would retrieve the {@link Keyset} from the
 * given {@link Supplier} to delegate cryptographic operations.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@NullMarked
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class KonfigyrKeysetOperations implements KeysetOperations {

	private final Supplier<@Nullable Keyset> supplier;

	@Override
	public ByteArray encrypt(ByteArray data) {
		return get().encrypt(data);
	}

	@Override
	public ByteArray encrypt(ByteArray data, @Nullable ByteArray context) {
		return get().encrypt(data, context);
	}

	@Override
	public ByteArray decrypt(ByteArray cipher) {
		return get().decrypt(cipher);
	}

	@Override
	public ByteArray decrypt(ByteArray cipher, @Nullable ByteArray context) {
		return get().decrypt(cipher, context);
	}

	@Override
	public ByteArray sign(ByteArray data) {
		return get().sign(data);
	}

	@Override
	public boolean verify(ByteArray signature, ByteArray data) {
		return get().verify(signature, data);
	}

	private synchronized Keyset get() {
		final Keyset keyset = supplier.get();
		Assert.notNull(keyset, "Delegating keyset for operations can not be null");
		return keyset;
	}
}
