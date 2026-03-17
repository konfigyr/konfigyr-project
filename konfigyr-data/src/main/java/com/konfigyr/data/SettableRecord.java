package com.konfigyr.data;

import lombok.EqualsAndHashCode;
import org.jooq.*;
import org.jooq.Record;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Utility class used to provide setters for the jOOQ {@link Record}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@NullMarked
@EqualsAndHashCode
public final class SettableRecord implements Supplier<Record> {

	private final Record delegate;

	/**
	 * Creates a new {@link SettableRecord} using the given {@link DSLContext} and {@link Table}
	 * for which the target {@link Record} would be created.
	 *
	 * @param context DSL context, can't be {@literal null}
	 * @param table target table, can't be {@literal null}
	 * @return the settable record, never {@literal null}
	 */
	public static SettableRecord of(DSLContext context, Table<?> table) {
		return of(context.newRecord(table));
	}

	/**
	 * Creates a new {@link SettableRecord} using the {@link Table} for which the target {@link Record}
	 * would be created.
	 *
	 * @param table target table, can't be {@literal null}
	 * @return the settable record, never {@literal null}
	 */
	public static SettableRecord of(Table<?> table) {
		return of(table.newRecord());
	}

	/**
	 * Creates a new {@link SettableRecord} using the given target {@link Record}.
	 *
	 * @param record target record, can't be {@literal null}
	 * @return the settable record, never {@literal null}
	 */
	public static SettableRecord of(Record record) {
		return new SettableRecord(record);
	}

	private SettableRecord(Record record) {
		this.delegate = record;
	}

	/**
	 * Set a value into the target {@link Record} for the given {@link Field}.
	 *
	 * @param <T> The generic field parameter
	 * @param field The field that should be set
	 * @param value The value that should be set
	 * @return the settable record, never {@literal null}
	 * @see Record#set(Field, Object) for more information
	 */
	public <T> SettableRecord set(Field<T> field, @Nullable T value) {
		delegate.set(field, value);
		return this;
	}

	/**
	 * Set a value into the target {@link Record} for the given {@link Field} <strong>only</strong>
	 * if the {@link Optional} is not empty.
	 *
	 * @param <T> The generic field parameter
	 * @param field The field that should be set
	 * @param value The value that should be set
	 * @return the settable record, never {@literal null}
	 * @see Record#set(Field, Object) for more information
	 */
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public <T> SettableRecord set(Field<T> field, Optional<? extends @Nullable T> value) {
		value.ifPresent(it -> delegate.set(field, it));
		return this;
	}

	/**
	 * Set a value into the target {@link Record} for the given {@link Field}.
	 *
	 * @param <T> The generic field parameter
	 * @param <U> The generic value parameter
	 * @param field The field that should be set
	 * @param value The value that should be set
	 * @param converter The converter function used to convert <code>value</code> into the field type
	 * @return the settable record, never {@literal null}
	 */
	public <T, U> SettableRecord set(Field<T> field, @Nullable U value, Function<? super U, ? extends @Nullable T> converter) {
		return set(field, Optional.ofNullable(value).map(converter));
	}

	/**
	 * Set a value into the target {@link Record} for the given {@link Field}.
	 *
	 * @param <T> The generic field parameter
	 * @param <U> The generic value parameter
	 * @param field The field that should be set
	 * @param value The value that should be set
	 * @param converter The converter used to convert <code>value</code> into the field type
	 * @return the settable record, never {@literal null}
	 * @see Record#set(Field, Object, Converter) for more information
	 */
	public <T, U> SettableRecord set(Field<T> field, @Nullable U value, Converter<? extends T, ? super @Nullable U> converter) {
		delegate.set(field, value, converter);
		return this;
	}

	public SettableRecord with(Consumer<SettableRecord> consumer) {
		consumer.accept(this);
		return this;
	}

	public SettableRecord with(UnaryOperator<SettableRecord> consumer) {
		return consumer.apply(this);
	}

	/**
	 * Retrieve the underlying the {@link Record} for which the values are set.
	 *
	 * @return the target record, never {@literal null}
	 */
	@Override
	public Record get() {
		return delegate;
	}

	@Override
	public String toString() {
		return delegate.toString();
	}
}
