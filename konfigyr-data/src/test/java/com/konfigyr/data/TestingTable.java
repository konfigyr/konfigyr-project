package com.konfigyr.data;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.time.OffsetDateTime;

/**
 * Manually defined jOOQ Table that is used for testing across the data module.
 * <p>
 * The Liquibase migration that contains schema for this table can be found here:
 * <code>migrations/test-changelog.xml</code>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
final class TestingTable extends TableImpl<Record> {

	static final TestingTable TESTING_TABLE = new TestingTable();

	final Field<Long> ID = createField(DSL.name("id"), SQLDataType.BIGINT, this);
	final Field<String> STATUS = createField(DSL.name("status"), SQLDataType.VARCHAR, this);
	final Field<OffsetDateTime> TIMESTAMP = createField(DSL.name("timestamp"), SQLDataType.TIMESTAMPWITHTIMEZONE, this);

	TestingTable() {
		super(DSL.name("testing_table"), null);
	}
}
