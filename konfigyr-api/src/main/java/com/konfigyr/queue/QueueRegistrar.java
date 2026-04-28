package com.konfigyr.queue;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.*;

/**
 * Central registry for resolving {@link QueueConfiguration} instances by queue name.
 * <p>
 * The {@link QueueRegistrar} acts as the lookup component within the queue infrastructure,
 * providing access to the runtime configuration associated with a particular logical queue.
 * It is typically initialized at application startup by collecting all
 * {@link QueueProcessorRegistration} beans and transforming them into immutable
 * {@link QueueConfiguration} instances.
 * <p>
 * Each entry in the registry is keyed by a unique queue name, which must correspond to the
 * identifier used when enqueueing tasks into the underlying work queue. This ensures that tasks
 * consumed from the database can be correctly routed to their associated processor and
 * execution configuration.
 * <p>
 * The registry is read-only at runtime and is designed to be safely shared across threads.
 * It is used by components such as schedulers and workers to:
 * <ul>
 *     <li>Resolve the {@link QueueProcessor} responsible for executing a task</li>
 *     <li>Access execution parameters such as timeout and backoff</li>
 *     <li>Obtain the dedicated {@link org.springframework.core.task.TaskExecutor}</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class QueueRegistrar implements Iterable<QueueConfiguration> {

	private final Map<String, QueueConfiguration> registry;

	/**
	 * Creates a new {@link QueueRegistrar} from the given {@link QueueProcessorRegistration} beans.
	 *
	 * @param registrations the queue processor registrations, must not be {@literal null}
	 * @return the queue registrar, never {@literal null}
	 */
	static QueueRegistrar of(QueueProcessorRegistration... registrations) {
		return of(Arrays.asList(registrations));
	}

	/**
	 * Creates a new {@link QueueRegistrar} from the given {@link QueueProcessorRegistration} beans.
	 *
	 * @param registrations the queue processor registrations, must not be {@literal null}
	 * @return the queue registrar, never {@literal null}
	 */
	static QueueRegistrar of(Iterable<QueueProcessorRegistration> registrations) {
		final Map<String, QueueConfiguration> registry = new LinkedHashMap<>();

		for (QueueProcessorRegistration registration : registrations) {
			if (registry.containsKey(registration.queueName())) {
				throw new IllegalStateException("Queue processor registration for queue '" + registration.queueName() +
						"' is already registered. Please make sure that your registrations for queues are unique.");
			}

			registry.put(registration.queueName(), registration.materialize());
		}

		registrations.forEach(registration -> registry.put(registration.queueName(), registration.materialize()));
		return new QueueRegistrar(Collections.unmodifiableMap(registry));
	}

	/**
	 * Returns the {@link QueueConfiguration} associated with the given queue name.
	 * <p>
	 * This method is typically used by scheduling and worker components to resolve the
	 * configuration required to execute a queued task. If no configuration is found, this
	 * indicates a configuration error and an {@link IllegalStateException} is thrown.
	 *
	 * @param queueName the logical name of the queue, must not be {@literal null}
	 * @return the corresponding {@link QueueConfiguration}, may be {@literal null} if not found
	 * @throws IllegalStateException if no configuration is found for the given queue name
	 */
	QueueConfiguration get(String queueName) {
		final QueueConfiguration configuration = registry.get(queueName);

		if (configuration == null) {
			throw new IllegalStateException("No queue configuration registered for queue " + queueName);
		}

		return configuration;
	}

	@Override
	public Iterator<QueueConfiguration> iterator() {
		return registry.values().iterator();
	}
}
