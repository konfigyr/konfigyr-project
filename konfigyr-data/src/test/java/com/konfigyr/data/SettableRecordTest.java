package com.konfigyr.data;

import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.konfigyr.data.TestingTable.TESTING_TABLE;

import static org.assertj.core.api.Assertions.*;

class SettableRecordTest {

	DSLContext context;

	@BeforeEach
	void setup() {
		MockConnection connection = new MockConnection(ctx -> null);
		context = DSL.using(connection, SQLDialect.POSTGRES);
	}

	@Test
	@DisplayName("should set a simple value")
	void setSimpleValues() {
		final var record = SettableRecord.of(context, TESTING_TABLE)
				.set(TESTING_TABLE.ID, 1L)
				.set(TESTING_TABLE.STATUS, "ACTIVE")
				.set(TESTING_TABLE.TIMESTAMP, OffsetDateTime.MIN);

		assertThat(record.get())
				.returns(true, Record::changed)
				.returns(1L, it -> it.get(TESTING_TABLE.ID))
				.returns("ACTIVE", it -> it.get(TESTING_TABLE.STATUS))
				.returns(OffsetDateTime.MIN, it -> it.get(TESTING_TABLE.TIMESTAMP));
	}

	@Test
	@DisplayName("should set an optional value")
	void setOptionalValue() {
		final var record = SettableRecord.of(TESTING_TABLE)
				.set(TESTING_TABLE.ID, Optional.of(1L))
				.set(TESTING_TABLE.STATUS, (String) null)
				.set(TESTING_TABLE.TIMESTAMP, Optional.empty());

		assertThat(record.get())
				.returns(true, Record::changed)
				.returns(1L, it -> it.get(TESTING_TABLE.ID))
				.returns(true, it -> it.changed(TESTING_TABLE.STATUS))
				.returns(false, it -> it.changed(TESTING_TABLE.TIMESTAMP));
	}

	@Test
	@DisplayName("should set a value with converter")
	void setConvertableValue() {
		final var timestamp = Instant.now();

		final var converter = Converter.of(
				OffsetDateTime.class,
				Instant.class,
				OffsetDateTime::toInstant,
				instant -> instant.atOffset(ZoneOffset.UTC)
		);

		final var record = SettableRecord.of(TESTING_TABLE.newRecord())
				.set(TESTING_TABLE.TIMESTAMP, timestamp, converter);

		assertThat(record.get())
				.returns(true, Record::changed)
				.returns(false, it -> it.changed(TESTING_TABLE.ID))
				.returns(false, it -> it.changed(TESTING_TABLE.STATUS))
				.returns(converter.to(timestamp), it -> it.get(TESTING_TABLE.TIMESTAMP));
	}

	@Test
	@DisplayName("should set a value with functional converter")
	void setConvertableValueWithFunction() {
		final var timestamp = Instant.now();

		final var record = SettableRecord.of(TESTING_TABLE.newRecord())
				.set(TESTING_TABLE.TIMESTAMP, timestamp, it -> it.atOffset(ZoneOffset.UTC));

		assertThat(record.get())
				.returns(true, Record::changed)
				.returns(false, it -> it.changed(TESTING_TABLE.ID))
				.returns(false, it -> it.changed(TESTING_TABLE.STATUS))
				.returns(timestamp.atOffset(ZoneOffset.UTC), it -> it.get(TESTING_TABLE.TIMESTAMP));
	}

	@Test
	@DisplayName("should configure record with lambda functions")
	void shouldConfigureRecordWithLambdas() {
		final var record = SettableRecord.of(TESTING_TABLE.newRecord()).with(it -> {
			it.set(TESTING_TABLE.ID, 23L);
		}).with(it -> {
			it.set(TESTING_TABLE.STATUS, "UPDATED");
		});

		assertThat(record.get())
				.returns(true, Record::changed)
				.returns(true, it -> it.changed(TESTING_TABLE.ID))
				.returns(true, it -> it.changed(TESTING_TABLE.STATUS))
				.returns(23L, it -> it.get(TESTING_TABLE.ID))
				.returns("UPDATED", it -> it.get(TESTING_TABLE.STATUS));
	}

	@Test
	@DisplayName("should fail to set a value to a field that does not exist in the record")
	void failToSetUnknownField() {
		assertThatThrownBy(() -> SettableRecord.of(context.newRecord()).set(TESTING_TABLE.ID, 1L))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("should check generated methods")
	void checkGeneratedMethods() {
		final var record = SettableRecord.of(context, TESTING_TABLE);

		assertThat(record)
				.isEqualTo(SettableRecord.of(context, TESTING_TABLE))
				.hasSameHashCodeAs(SettableRecord.of(context, TESTING_TABLE))
				.hasToString(record.get().toString());

		record.set(TESTING_TABLE.STATUS, "CHANGED");

		assertThat(record)
				.isNotEqualTo(SettableRecord.of(context, TESTING_TABLE))
				.doesNotHaveSameHashCodeAs(SettableRecord.of(context, TESTING_TABLE));
	}

}
