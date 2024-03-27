package com.konfigyr.crypto;

import com.konfigyr.io.ByteArray;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.function.Supplier;

/**
 * Simple implementation of the {@link KeysetOperations} that would retrieve the {@link Keyset} from the
 * given {@link Supplier} to delegate cryptographic operations.
 *
 * @author : Vladimir Spasic
 * @since : 27.03.24, Wed
 **/
@RequiredArgsConstructor
final class KonfigyrKeysetOperations implements KeysetOperations {

	private final Supplier<Keyset> supplier;

	@NonNull
	@Override
	public ByteArray encrypt(@NonNull ByteArray data) {
		return get().encrypt(data);
	}

	@NonNull
	@Override
	public ByteArray encrypt(@NonNull ByteArray data, ByteArray context) {
		return get().encrypt(data, context);
	}

	@NonNull
	@Override
	public ByteArray decrypt(@NonNull ByteArray cipher) {
		return get().decrypt(cipher);
	}

	@NonNull
	@Override
	public ByteArray decrypt(@NonNull ByteArray cipher, ByteArray context) {
		return get().decrypt(cipher, context);
	}

	@NonNull
	@Override
	public ByteArray sign(@NonNull ByteArray data) {
		return get().sign(data);
	}

	@Override
	public boolean verify(@NonNull ByteArray signature, @NonNull ByteArray data) {
		return get().verify(signature, data);
	}

	@NonNull
	private synchronized Keyset get() {
		final Keyset keyset = supplier.get();
		Assert.notNull(keyset, "Delegating keyset for operations can not be null");
		return keyset;
	}
}
