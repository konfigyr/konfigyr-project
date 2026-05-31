package com.konfigyr.kms;

import com.konfigyr.entity.EntityId;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jspecify.annotations.NullMarked;
import org.springframework.util.Assert;

@ToString
@NullMarked
@EqualsAndHashCode
public sealed abstract class KeyOperation permits KeyOperation.DeactivateKey, KeyOperation.ReactivateKey,
		KeyOperation.CompromiseKey, KeyOperation.RestoreKey, KeyOperation.DestroyKey {

	protected final EntityId keyset;
	protected final String key;

	protected KeyOperation(EntityId keyset, String key) {
		Assert.notNull(keyset, "Keyset identifier must not be null");
		Assert.hasText(key, "Key identifier must not be empty");
		this.keyset = keyset;
		this.key = key;
	}

	/**
	 * The unique identifier of the {@link KeysetMetadata} that owns the {@link KeyMetadata}.
	 *
	 * @return the keyset identifier, never {@literal null}.
	 */
	public EntityId keyset() {
		return keyset;
	}

	/**
	 * The unique identifier of the {@link KeyMetadata} that is the subject of the operation.
	 *
	 * @return the key identifier, never {@literal null}.
	 */
	public String key() {
		return key;
	}

	public static KeyOperation deactivate(EntityId keyset, String key) {
		return new DeactivateKey(keyset, key);
	}

	public static KeyOperation reactivate(EntityId keyset, String key) {
		return new ReactivateKey(keyset, key);
	}

	public static KeyOperation compromise(EntityId keyset, String key) {
		return new CompromiseKey(keyset, key);
	}

	public static KeyOperation restore(EntityId keyset, String key) {
		return new RestoreKey(keyset, key);
	}

	public static KeyOperation destroy(EntityId keyset, String key) {
		return new DestroyKey(keyset, key);
	}

	static final class DeactivateKey extends KeyOperation {
		private DeactivateKey(EntityId keyset, String key) {
			super(keyset, key);
		}
	}

	static final class ReactivateKey extends KeyOperation {
		private ReactivateKey(EntityId keyset, String key) {
			super(keyset, key);
		}
	}

	static final class CompromiseKey extends KeyOperation {
		private CompromiseKey(EntityId keyset, String key) {
			super(keyset, key);
		}
	}

	static final class RestoreKey extends KeyOperation {
		private RestoreKey(EntityId keyset, String key) {
			super(keyset, key);
		}
	}

	static final class DestroyKey extends KeyOperation {
		private DestroyKey(EntityId keyset, String key) {
			super(keyset, key);
		}
	}

}
