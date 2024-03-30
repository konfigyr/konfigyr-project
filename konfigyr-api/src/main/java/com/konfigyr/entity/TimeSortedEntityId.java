package com.konfigyr.entity;

import io.hypersistence.tsid.TSID;
import lombok.EqualsAndHashCode;
import org.springframework.lang.NonNull;

import java.io.Serial;

/**
 * Implementation of the {@link EntityId} that is based on the {@link TSID}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see TSID
 * @see TimeSortedEntityIdProvider
 **/
@EqualsAndHashCode(of = "id")
final class TimeSortedEntityId implements EntityId {

	@Serial
	private static final long serialVersionUID = -6631425677891239174L;

	private final long id;
	private final String hash;

	TimeSortedEntityId(@NonNull TSID value) {
		this.id = value.toLong();
		this.hash = value.toString();
	}

	@Override
	public long get() {
		return id;
	}

	@NonNull
	@Override
	public String serialize() {
		return hash;
	}

	@Override
	public int compareTo(@NonNull EntityId entityId) {
		return Long.compare(get(), entityId.get());
	}

	@Override
	public String toString() {
		return "EntityId(" + id + ", " + hash + ")";
	}
}
