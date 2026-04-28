package com.konfigyr.queue;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.ClassUtils;

import java.time.Duration;

/**
 * Immutable runtime configuration for a registered scheduled queue that is materialized from
 * a {@link QueueProcessorRegistration}.
 * <p>
 * The {@link QueueConfiguration} encapsulates all execution-related concerns required by the
 * scheduling infrastructure, including retry behavior, timeout constraints, and task execution.
 * It represents a fully resolved and validated configuration that can be safely used at runtime.
 * <p>
 * Instances of this type are typically created during application startup by transforming
 * {@link QueueProcessorRegistration} beans into a normalized form with all defaults applied.
 * <p>
 * This type is intentionally immutable to ensure consistent behavior during task execution and
 * to avoid accidental modification of queue configuration at runtime.
 *
 * @param backoff the duration applied when rescheduling failed tasks, never {@literal null}
 * @param timeout the maximum allowed execution time for a single task, never {@literal null}
 * @param taskExecutor the executor used to run tasks asynchronously, never {@literal null}
 * @param queueProcessor the processor responsible for executing tasks, never {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see QueueRegistrar
 */
@NullMarked
record QueueConfiguration(
		Duration backoff,
		Duration timeout,
		TaskExecutor taskExecutor,
		QueueProcessor queueProcessor
) {

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("backoff", backoff)
				.append("timeout", timeout)
				.append("taskExecutor", ClassUtils.getQualifiedName(taskExecutor.getClass()))
				.append("queueProcessor", ClassUtils.getQualifiedName(queueProcessor.getClass()))
				.toString();
	}

}
