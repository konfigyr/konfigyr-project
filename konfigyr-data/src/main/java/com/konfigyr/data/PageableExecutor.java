package com.konfigyr.data;

import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.jooq.Record;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.LongSupplier;

/**
 * jOOQ support class for {@link Query} execution using {@link Pageable}. It would apply the pagination
 * instructions and the sorting conditions to the jOOQ {@link Query} before fetching the result set.
 * <p>
 * This utility class would make use of the {@link PageableExecutionUtils} that applies page optimizations.
 * <p>
 * To create new instances of this class use the fluent {@link Builder} and specify your sortable
 * field mappings so that the executor can apply them from {@link Sort} instructions.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see PageableExecutionUtils
 **/
@RequiredArgsConstructor
public class PageableExecutor {

	private final Map<String, Field<?>> sortFields;
	private final OrderField<?> defaultSortField;

	/**
	 * Creates the fluent {@link PageableExecutor} builder to customize the sortable field mappings.
	 * @return pageable executor builder, never {@literal null}.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Applies the {@link Pageable} instructions and executes the query to construct a {@link Page}
	 * based on the retrieved result set, {@link Pageable} and {@link LongSupplier} applying optimizations
	 * via {@link PageableExecutionUtils}.
	 *
	 * @param query query to be executed, must not be {@literal null}.
	 * @param mapper result set mapper applied on the retrieved record result set, must not be {@literal null}.
	 * @param pageable must not be {@literal null} but can be {@link Pageable#unpaged()}.
	 * @param totalSupplier must not be {@literal null}.
	 * @param <T> page content type that would be returned from the record converter
	 * @return the {@link Page} for the retrieved and converted result set and a total size.
	 */
	@NonNull
	public <T> Page<T> execute(
			@NonNull SelectOrderByStep<? extends Record> query,
			@NonNull Converter<Record, T> mapper,
			@NonNull Pageable pageable,
			@NonNull LongSupplier totalSupplier
	) {
		final List<T> results = applyPageable(query, pageable)
				.fetch(mapper::convert);

		return PageableExecutionUtils.getPage(results, pageable, totalSupplier);
	}

	private <R extends Record> ResultQuery<R> applyPageable(SelectOrderByStep<R> query, Pageable pageable) {
		final Collection<OrderField<?>> orderBy = createOrderBy(pageable.getSort());

		if (pageable.isUnpaged()) {
			return query.orderBy(orderBy);
		}

		return query.orderBy(orderBy)
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize());
	}

	private Collection<OrderField<?>> createOrderBy(Sort sort) {
		if (sort == null || sort.isUnsorted() || sortFields.isEmpty()) {
			return Collections.singleton(defaultSortField);
		}

		final List<OrderField<?>> sorts = new ArrayList<>();

		sort.forEach(order -> {
			final Field<?> field = sortFields.get(order.getProperty());

			if (field != null) {
				sorts.add(field.sort(order.isAscending() ? SortOrder.ASC : SortOrder.DESC));
			}
		});

		if (sorts.isEmpty()) {
			return Collections.singleton(defaultSortField);
		}

		return sorts;
	}

	public static final class Builder {
		private final Map<String, Field<?>> sortFields = new LinkedHashMap<>();
		private OrderField<?> defaultSortField;

		/**
		 * Specify the default sortable {@link Field} that would be applied if the {@link Sort}
		 * is not sorted or no configured sortable field is present.
		 *
		 * @param field field to be sorted using the given {@link SortOrder}
		 * @param order sort order to be applied to the {@link Field}
		 * @return pageable executor builder, never {@literal null}.
		 */
		public Builder defaultSortField(Field<?> field, SortOrder order) {
			Assert.notNull(field, "Default field for ordering can not be null");
			Assert.notNull(order, "Sort order can not be null");

			return defaultSortField(field.sort(order));
		}

		/**
		 * Specify the default sortable {@link SortField} that would be applied if the {@link Sort}
		 * is not sorted or no configured sortable field is present.
		 *
		 * @param field default sort field
		 * @return pageable executor builder, never {@literal null}.
		 */
		public Builder defaultSortField(SortField<?> field) {
			Assert.notNull(field, "Default sort field can not be null");
			this.defaultSortField = field;
			return this;
		}

		/**
		 * Specify the sortable {@link Field} that would be added to the {@link Query} if
		 * {@link Sort sorting instructions} contain a {@link Sort.Order} that matches the
		 * given parameter name.
		 *
		 * @param parameter sortable parameter name in the {@link Sort sorting instructions}.
		 * @param field field to be sorted based on the {@link Sort.Direction} retrieved from the {@link Sort.Order}.
		 * @return pageable executor builder, never {@literal null}.
		 */
		public Builder sortField(String parameter, Field<?> field) {
			Assert.hasText(parameter, "Sort field parameter name can not be blank");
			Assert.notNull(field, "Field to be sorted can not be null");

			this.sortFields.put(parameter, field);
			return this;
		}

		public PageableExecutor build() {
			Assert.notNull(defaultSortField, "Default sort field is required");

			return new PageableExecutor(Collections.unmodifiableMap(sortFields), defaultSortField);
		}
	}
}
