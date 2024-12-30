package com.konfigyr.crypto;

import org.springframework.lang.NonNull;

/**
 * Factory interface that is used to create, or resolve any existing, {@link KeysetOperations} for
 * a given {@link KeysetDefinition}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@FunctionalInterface
public interface KeysetOperationsFactory {

	/**
	 * Creates a {@link KeysetOperations} interface that can be used to perform cryptographic
	 * operations on the {@link Keyset} that is described by the given {@link KeysetDefinition}.
	 *
	 * @param definition definition used to describe the {@link Keyset}, can't be {@literal null}
	 * @return Keyset operations instance, never {@literal null}.
	 */
	@NonNull
	KeysetOperations create(@NonNull KeysetDefinition definition);

}
