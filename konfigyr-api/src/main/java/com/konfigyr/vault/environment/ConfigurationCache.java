package com.konfigyr.vault.environment;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.github.benmanes.caffeine.cache.Weigher;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceEvent;
import com.konfigyr.vault.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.function.ThrowingSupplier;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cache used to store the sealed configuration {@link Properties} state using Caffeine.
 * <p>
 * <b>Key design principles:</b>
 * <ul>
 *     <li>Uses {@code maximumWeight} instead of {@code maximumSize} to approximate real memory usage.</li>
 *     <li>Custom {@link Weigher} estimates heap footprint (key and value).</li>
 *     <li>Relies on Caffeine's W-TinyLFU eviction policy (default) for optimal hit rates under skewed workloads.</li>
 *     <li>Stores only sealed values (no plaintext) to preserve cryptographic boundaries.</li>
 * </ul>
 * <p>
 * <b>Eviction guarantees:</b>
 * <ul>
 *     <li>Profile update → explicit invalidation or refresh</li>
 *     <li>Profile removal → explicit invalidation</li>
 *     <li>Service deletion → bulk invalidation by serviceId</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
class ConfigurationCache implements MeterBinder {

	static final String CACHE_NAME = "vault.configuration-state-cache";

	private final Cache<CacheKey, Properties> cache;

	ConfigurationCache(String spec) {
		this(CaffeineSpec.parse(spec));
	}

	ConfigurationCache(CaffeineSpec spec) {
		this.cache = Caffeine.from(spec)
				.weigher(new PropertiesWeigher())
				.recordStats()
				.build();
	}

	/**
	 * Retrieves a cached configuration state or loads from the {@code supplier} if absent.
	 *
	 * @param service  the service that owns the profile
	 * @param profile  the profile for which to retrieve the configuration state
	 * @param supplier function that would load the configuration state if absent
	 * @return cached or loaded Properties, never {@literal null}
	 */
	Properties get(Service service, Profile profile, ThrowingSupplier<Properties> supplier) {
		final CacheKey key = new CacheKey(service, profile);
		return cache.get(key, ignore -> supplier.get());
	}

	/**
	 * Checks if a cached configuration state exists for the given {@code profile}.
	 *
	 * @param service the service that owns the profile
	 * @param profile the profile for which to check the configuration state
	 * @return {@code true} if the configuration state exists, {@code false} otherwise
	 */
	boolean has(Service service, Profile profile) {
		final CacheKey key = new CacheKey(service, profile);
		return cache.getIfPresent(key) != null;
	}

	/**
	 * Stores a configuration state in the cache.
	 *
	 * @param service the service that owns the profile
	 * @param profile the profile for which to store the configuration state
	 * @param state   the configuration state to store
	 * @return the stored configuration state, never {@literal null}
	 */
	Properties put(Service service, Profile profile, Properties state) {
		cache.put(new CacheKey(service, profile), state);
		return state;
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		CaffeineCacheMetrics.monitor(registry, cache, CACHE_NAME);
	}

	@Async
	@EventListener(id = "vault.configuration-cache.service-deleted", classes = ServiceEvent.Deleted.class)
	void on(ServiceEvent.Deleted event) {
		final EntityId service = event.id();

		final Map<CacheKey, Properties> map = cache.asMap();
		map.keySet().removeIf(key -> key.service() == service.get());
	}

	@Async
	@EventListener(id = "vault.configuration-cache.profile-deleted", classes = ProfileEvent.Deleted.class)
	void on(ProfileEvent.Deleted event) {
		final Profile profile = event.get();
		cache.invalidate(new CacheKey(profile));
	}

	@Async
	@EventListener(id = "vault.configuration-cache.changes-applied", classes = VaultEvent.ChangesApplied.class)
	void on(VaultEvent.ChangesApplied event) {
		final Profile profile = event.get();
		cache.invalidate(new CacheKey(profile));
	}

	record CacheKey(long service, long profile) implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		CacheKey(Profile profile) {
			this(profile.service().get(), profile.id().get());
		}

		CacheKey(Service service, Profile profile) {
			this(service.id().get(), profile.id().get());
		}
	}

	/**
	 * A {@link Weigher} implementation that estimates the in-heap memory footprint of a cached
	 * {@link Properties} object within the Caffeine {@link Cache}.
	 * <p>
	 * It is <b>important</b> to realize that these generated weights are an <i>approximation</i>,
	 * not an exact measurement.
	 * <p>
	 * Caffeine does not track actual JVM heap usage. Instead, it relies on a relative <i>weight</i>
	 * per cache entry. The goal of this weigher is therefore to:
	 * <ul>
	 *     <li>Maintain a <b>consistent proportional relationship</b> between entries</li>
	 *     <li>Approximate real memory usage closely enough to avoid cache overgrowth</li>
	 *     <li>Prefer <b>slight overestimation</b> rather than underestimation (safer)</li>
	 * </ul>
	 *
	 * <p>
	 * Why overestimate the weights, you may ask. If we use underestimation, the cache may exceed
	 * intended memory bounds, that further adds GC pressure and OOM risk. Overestimation is a
	 * slightly more aggressive eviction strategy, thus can be considered safer and predictable.
	 * <p>
	 * This implementation models the memory layout of objects based on typical HotSpot JVM
	 * characteristics (64-bit with compressed OOPs):
	 * <ul>
	 *     <li>Object header: ~12–16 bytes (rounded up due to alignment)</li>
	 *     <li>Array header: ~16 bytes</li>
	 *     <li>Primitive sizes: long = 8 bytes</li>
	 * </ul>
	 */
	record PropertiesWeigher() implements Weigher<CacheKey, Properties> {

		@Override
		public int weigh(CacheKey key, Properties properties) {
			/*
			 * Base cost for the cache key:
			 *
			 * CacheKey consists of:
			 *  - object header → ~16 bytes
			 *  - two long fields → 8 * 2 bytes
			 */
			final AtomicInteger weight = new AtomicInteger(32);

			properties.forEachProperty((name, value) -> {
				/*
				 * Per-property weight breakdown:
				 *
				 * 1. Property container / entry object (~32 bytes)
				 *    - includes object header + references
				 *
				 * 2. Property name (String)
				 *    - Estimated as: 40 + (length × 2)
				 *
				 *      Explanation:
				 *      - ~40 bytes = String object + backing array overhead
				 *      - length × 2 = UTF-16 worst-case (2 bytes per char)
				 *
				 *      Note:
				 *      - Modern JVMs (Java 9+) may use compact strings (1 byte per char),
				 *        but we intentionally assume 2 bytes per char as a safe upper bound.
				 *
				 * 3. Property value and checksum (byte array)
				 *    - Estimated as: 16 + size
				 *
				 *      Explanation:
				 *      - 16 bytes = array header
				 *      - size = actual byte[] length (already binary, no encoding overhead)
				 */
				weight.addAndGet(32 + weigh(name) + weigh(value.get()) + weigh(value.checksum()));
			});

			return weight.get();
		}

		private int weigh(String value) {
			return 40 + (value.length() * 2);
		}

		private int weigh(ByteArray value) {
			return 16 + value.size();
		}
	}

}
