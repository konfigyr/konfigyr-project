package com.konfigyr.jooq;

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

import static org.assertj.core.api.Assertions.*;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;

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
		final var record = SettableRecord.of(context, ACCOUNTS)
				.set(ACCOUNTS.ID, 1L)
				.set(ACCOUNTS.STATUS, "ACTIVE")
				.set(ACCOUNTS.LAST_LOGIN_AT, OffsetDateTime.MIN);

		assertThat(record.get())
				.returns(true, Record::changed)
				.returns(1L, it -> it.get(ACCOUNTS.ID))
				.returns("ACTIVE", it -> it.get(ACCOUNTS.STATUS))
				.returns(OffsetDateTime.MIN, it -> it.get(ACCOUNTS.LAST_LOGIN_AT));
	}

	@Test
	@DisplayName("should set an optional value")
	void setOptionalValue() {
		final var record = SettableRecord.of(ACCOUNTS)
				.set(ACCOUNTS.ID, Optional.of(1L))
				.set(ACCOUNTS.STATUS, (String) null)
				.set(ACCOUNTS.LAST_LOGIN_AT, Optional.empty());

		assertThat(record.get())
				.returns(true, Record::changed)
				.returns(1L, it -> it.get(ACCOUNTS.ID))
				.returns(true, it -> it.changed(ACCOUNTS.STATUS))
				.returns(false, it -> it.changed(ACCOUNTS.LAST_LOGIN_AT));
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

		final var record = SettableRecord.of(ACCOUNTS.newRecord())
				.set(ACCOUNTS.LAST_LOGIN_AT, timestamp, converter);

		assertThat(record.get())
				.returns(true, Record::changed)
				.returns(false, it -> it.changed(ACCOUNTS.ID))
				.returns(false, it -> it.changed(ACCOUNTS.STATUS))
				.returns(converter.to(timestamp), it -> it.get(ACCOUNTS.LAST_LOGIN_AT));
	}

	@Test
	@DisplayName("should set a value with functional converter")
	void setConvertableValueWithFunction() {
		final var timestamp = Instant.now();

		final var record = SettableRecord.of(ACCOUNTS.newRecord())
				.set(ACCOUNTS.LAST_LOGIN_AT, timestamp, it -> it.atOffset(ZoneOffset.UTC));

		assertThat(record.get())
				.returns(true, Record::changed)
				.returns(false, it -> it.changed(ACCOUNTS.ID))
				.returns(false, it -> it.changed(ACCOUNTS.STATUS))
				.returns(timestamp.atOffset(ZoneOffset.UTC), it -> it.get(ACCOUNTS.LAST_LOGIN_AT));
	}

	@Test
	@DisplayName("should fail to set a value to a field that does not exist in the record")
	void failToSetUnknownField() {
		assertThatThrownBy(() -> SettableRecord.of(context.newRecord()).set(ACCOUNTS.ID, 1L))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("should check generated methods")
	void checkGeneratedMethods() {
		final var record = SettableRecord.of(context, ACCOUNTS);

		assertThat(record)
				.isEqualTo(SettableRecord.of(context, ACCOUNTS))
				.hasSameHashCodeAs(SettableRecord.of(context, ACCOUNTS))
				.hasToString(record.get().toString());

		record.set(ACCOUNTS.STATUS, "CHANGED");

		assertThat(record)
				.isNotEqualTo(SettableRecord.of(context, ACCOUNTS))
				.doesNotHaveSameHashCodeAs(SettableRecord.of(context, ACCOUNTS));
	}

}