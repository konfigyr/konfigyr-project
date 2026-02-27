package com.konfigyr.vault;

import org.jspecify.annotations.NullMarked;

/**
 * Interface that can be used to extend or customize the behavior of the {@link Vault} instances.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@FunctionalInterface
public interface VaultExtension {

	/**
	 * Extends the given {@link Vault} instance.
	 *
	 * @param vault instance to be extended, must not be {@literal null}.
	 * @return the extended vault instance.
	 */
	Vault extend(Vault vault);

	/**
	 * Returns a composed extension that first applies the {@code before} extension to the
	 * {@link Vault} instance, and then applies this function to the result.
	 * <p>
	 * If evaluation of either function throws an exception, it is relayed to the caller of
	 * the composed function.
	 *
	 * @param before the extension to apply before this extension is applied
	 * @return a composed extension that first applies the {@code before} extension and
	 * then applies this extension
	 */
	default VaultExtension compose(VaultExtension before) {
		return vault -> extend(before.extend(vault));
	}

}
