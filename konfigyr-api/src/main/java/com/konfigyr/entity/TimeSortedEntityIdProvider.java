package com.konfigyr.entity;

import io.hypersistence.tsid.TSID;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Implementation of the {@link EntityIdProvider} that creates instances of {@link EntityId} backed by
 * a {@link TSID} implementation.
 * <p>
 * The {@link TSID} is configured using the following options:
 * <ul>
 *     <li>
 *         Uses default configuration for the node part of the random component. You can change this
 *         configuration via environment variables or system properties. Please consult the
 *         <a href="https://github.com/vladmihalcea/hypersistence-tsid">TSID documentation</a> for
 *         more information.
 *     </li>
 *     <li>
 *         Second part of the random {@link TSID} component, apart from the above mentioned node part,
 *         is the counter. This implementation uses the {@link SecureRandom} to reset the counter when
 *         the millisecond changes.
 *     </li>
 *     <li>
 *         To generate the generate the time component f the {@link TSID}, a UTC system time {@link Clock}
 *         is used and is based on an epoch that starts from the following date: <code>2024-01-01</code>.
 *     </li>
 * </ul>
 * <p>
 * This class is considered as a default implementation of the {@link EntityIdProvider} SPI. If there is no
 * specific implementation of the provider that is configured to be loaded by the {@link java.util.ServiceLoader},
 * this one would be used.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see TSID
 * @see <a href="https://github.com/vladmihalcea/hypersistence-tsid">TSID Docuentation</a>
 **/
final class TimeSortedEntityIdProvider implements EntityIdProvider {

	static final Supplier<EntityIdProvider> instance = SingletonSupplier.of(TimeSortedEntityIdProvider::new);

	static EntityIdProvider getInstance() {
		return instance.get();
	}

	private final TSID.Factory factory;

	TimeSortedEntityIdProvider() {
		this(
				new TSID.Factory.Builder()
						.withRandom(new SecureRandom())
						.withClock(Clock.systemUTC())
						.withCustomEpoch(Instant.parse("2024-01-01T00:00:00.000Z"))
						.build()
		);
	}

	TimeSortedEntityIdProvider(TSID.Factory factory) {
		this.factory = factory;
	}

	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE;
	}

	@NonNull
	@Override
	public synchronized EntityId generate() {
		return new TimeSortedEntityId(factory.generate());
	}

	@NonNull
	@Override
	public EntityId create(long id) {
		Assert.isTrue(id > 0, "Internal entity identifier must be a positive number: " + id);

		return new TimeSortedEntityId(TSID.from(id));
	}

	@NonNull
	@Override
	public EntityId create(@NonNull String hash) {
		Assert.isTrue(TSID.isValid(hash), "Invalid external entity identifier value: " + hash);

		return new TimeSortedEntityId(TSID.from(hash));
	}

}
