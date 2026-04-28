package com.konfigyr.queue;

import com.konfigyr.entity.EntityId;
import org.jooq.Record;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import static com.konfigyr.data.tables.WorkerQueue.WORKER_QUEUE;

/**
 * Record that represents a task that has been queued for execution.
 * <p>
 * It contains the unique identifier of the task, the name of the queue it was enqueued on, and the
 * identifier of the entity it represents.
 *
 * @param id the unique identifier of the task, can't be {@literal null}
 * @param queueName the name of the queue on which the task was enqueued, can't be {@literal null}
 * @param entityId the identifier of the entity the task represents, can't be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
record QueuedTask(UUID id, String queueName, EntityId entityId) implements Serializable {

	@Serial
	private static final long serialVersionUID = 3832218635659737900L;

	QueuedTask(Record record) {
		this(
				record.get(WORKER_QUEUE.ID),
				record.get(WORKER_QUEUE.QUEUE_NAME),
				record.get(WORKER_QUEUE.ENTITY_ID, EntityId.class)
		);
	}
}
