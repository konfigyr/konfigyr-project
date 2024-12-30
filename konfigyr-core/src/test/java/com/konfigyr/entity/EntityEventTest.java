package com.konfigyr.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Vladimir Spasic
 **/
class EntityEventTest {

	@Test
	@DisplayName("should create entity events")
	void shouldCreateEvents() {
		final var event = new TestEntityEvent(1);

		assertThat(event)
				.returns(EntityId.from(1), EntityEvent::id)
				.satisfies(it -> assertThat(it.timestamp())
						.isCloseTo(Instant.now(), byLessThan(600, ChronoUnit.MILLIS))
				)
				.isNotEqualTo(new TestEntityEvent(1))
				.hasSameHashCodeAs(event)
				.doesNotHaveSameHashCodeAs(new TestEntityEvent(1))
				.doesNotHaveToString(new TestEntityEvent(1).toString());
	}

	private static final class TestEntityEvent extends EntityEvent {
		private TestEntityEvent(long id) {
			super(EntityId.from(id));
		}
	}
}