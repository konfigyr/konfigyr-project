package com.konfigyr.entity;

import org.jmolecules.ddd.annotation.Factory;
import org.springframework.core.OrderComparator;
import org.jspecify.annotations.NonNull;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.SingletonSupplier;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * Singleton utility class that is used to load the configured {@link EntityIdProvider} SPI
 * via {@link ServiceLoader} to generate and create instances of the {@link EntityId}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Factory
final class EntityIdFactory {

	static final Supplier<EntityIdFactory> instance = SingletonSupplier.of(EntityIdFactory::new);

	static EntityIdFactory getInstance() {
		return instance.get();
	}

	private final EntityIdProvider provider;

	EntityIdFactory() {
		this(lookupEntityIdProvider());
	}

	EntityIdFactory(EntityIdProvider  provider) {
		this.provider = provider;
	}

	@NonNull
	Optional<EntityId> create() {
		return Optional.ofNullable(provider.generate());
	}

	@NonNull
	EntityId create(long id) {
		return provider.create(id);
	}

	@NonNull
	EntityId create(String hash) {
		return provider.create(hash);
	}

	@NonNull
	static EntityIdProvider lookupEntityIdProvider() {
		final ServiceLoader<EntityIdProvider> loader = ServiceLoader.load(EntityIdProvider.class,
				ClassUtils.getDefaultClassLoader());

		return loader.stream()
				.map(ServiceLoader.Provider::get)
				.min(OrderComparator.INSTANCE)
				.orElseGet(TimeSortedEntityIdProvider::getInstance);
	}

}
