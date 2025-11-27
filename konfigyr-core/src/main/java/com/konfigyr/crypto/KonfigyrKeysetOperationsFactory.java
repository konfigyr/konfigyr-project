package com.konfigyr.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.DisposableBean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The default implementation of the {@link KeysetOperationsFactory} that would create, and cache,
 * instances of {@link KeysetOperations}.
 * <p>
 * The {@link KeysetOperations} that are created by this factory would retrieve the {@link Keyset}
 * from the {@link KeysetStore} or create one if missing using the supplied {@link KeysetDefinition}
 * and the {@link KeyEncryptionKey master key encryption key} that is configured for this application.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Slf4j
@NullMarked
@RequiredArgsConstructor
class KonfigyrKeysetOperationsFactory implements KeysetOperationsFactory, DisposableBean {

	private final Map<KeysetDefinition, KeysetOperations> cache = new ConcurrentHashMap<>(4);

	private final KeysetStore store;

	@Override
	public KeysetOperations create(KeysetDefinition definition) {
		return cache.computeIfAbsent(definition, this::createKeysetOperations);
	}

	@Override
	public void destroy() {
		cache.clear();
	}

	private KeysetOperations createKeysetOperations(KeysetDefinition definition) {
		log.debug("Creating new Keyset operations instance for: {}", definition);

		return KeysetOperations.of(() -> {
			try {
				return store.read(definition.getName());
			} catch (CryptoException.KeysetNotFoundException e) {
				return createKeyset(definition);
			}
		});
	}

	protected Keyset createKeyset(KeysetDefinition definition) {
		final KeyEncryptionKey kek = store.kek(CryptoProperties.PROVIDER_NAME, CryptoProperties.KEK_ID);

		if (log.isDebugEnabled()) {
			log.debug("Creating a new Keyset that would be used by the Keyset operations using: [kek={}, definition={}",
					kek, definition);
		}

		return store.create(kek, definition);
	}
}
