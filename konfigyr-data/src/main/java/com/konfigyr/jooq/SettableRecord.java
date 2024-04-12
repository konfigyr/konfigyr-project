package com.konfigyr.jooq;

import lombok.EqualsAndHashCode;
import org.jooq.*;
import org.jooq.Record;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Utility class used to provide setters for the jOOQ {@link Record}.
 *
 * @author Vladimir Spasic
 **/
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
	@NonNull
	public static SettableRecord of(@NonNull DSLContext context, @NonNull Table<?> table) {
		return of(context.newRecord(table));
	}

	/**
	 * Creates a new {@link SettableRecord} using the {@link Table} for which the target {@link Record}
	 * would be created.
	 *
	 * @param table target table, can't be {@literal null}
	 * @return the settable record, never {@literal null}
	 */
	@NonNull
	public static SettableRecord of(@NonNull Table<?> table) {
		return of(table.newRecord());
	}

	/**
	 * Creates a new {@link SettableRecord} using the given target {@link Record}.
	 *
	 * @param record target record, can't be {@literal null}
	 * @return the settable record, never {@literal null}
	 */
	@NonNull
	public static SettableRecord of(@NonNull Record record) {
		return new SettableRecord(record);
	}

	private SettableRecord(@NonNull Record record) {
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
	@NonNull
	public <T> SettableRecord set(@NonNull Field<T> field, @Nullable T value) {
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
	@NonNull
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public <T> SettableRecord set(@NonNull Field<T> field, @NonNull Optional<? extends T> value) {
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
	 * @param converter The converter used to convert <code>value</code> into the field type
	 * @return the settable record, never {@literal null}
	 * @see Record#set(Field, Object, Converter) for more information
	 */
	@NonNull
	public <T, U> SettableRecord set(@NonNull Field<T> field, @Nullable U value, @NonNull Converter<? extends T, ? super U> converter) {
		delegate.set(field, value, converter);
		return this;
	}

	/**
	 * Retrieve the underlying the {@link Record} for which the values are set.
	 *
	 * @return the target record, never {@literal null}
	 */
	@NonNull
	@Override
	public Record get() {
		return delegate;
	}

	@Override
	public String toString() {
		return delegate.toString();
	}
}
