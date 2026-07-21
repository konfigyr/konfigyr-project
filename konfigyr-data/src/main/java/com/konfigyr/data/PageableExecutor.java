package com.konfigyr.data;

import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Supplier;

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
@NullMarked
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
	 * Derives a {@link PageableExecutor} that additionally matches and ranks results against a {@code tsvector}
	 * {@code field} using a full-text search {@code term}, sorting matches by {@code ts_rank} in descending
	 * order (most relevant first) ahead of any other sort.
	 * <p>
	 * See {@link #rankBy(String, Field, SortOrder)} for the full contract, including the requirement that
	 * {@code term} already be valid {@code tsquery} syntax.
	 *
	 * @param term the search term, already valid {@code tsquery} syntax, can be {@literal null} or blank
	 * @param field the {@code tsvector} field to match and rank against, can't be {@literal null}
	 * @return a ranked {@link PageableExecutor}, or this same executor if {@code term} is blank, never {@literal null}.
	 */
	public PageableExecutor rankBy(@Nullable String term, Field<?> field) {
		return rankBy(term, field, SortOrder.DESC);
	}

	/**
	 * Derives a {@link PageableExecutor} that additionally matches and ranks results against a {@code tsvector}
	 * {@code field} using a full-text search {@code term}.
	 * <p>
	 * The returned executor adds a {@code field @@ to_tsquery('simple', term)} condition to every query it
	 * executes (see {@link #execute(Supplier, Supplier, Converter, Pageable)}), and sorts by
	 * {@code ts_rank(field, to_tsquery('simple', term))} using the given {@code order}, ahead of whatever
	 * sort the {@link Pageable} or the configured default sort field would otherwise apply.
	 * <p>
	 * {@code term} is passed as-is to {@code to_tsquery}, which parses {@code tsquery} operator syntax
	 * (<code>&amp;</code>, <code>|</code>, <code>:*</code>, ...) rather than free text — callers must
	 * already have converted a raw user-supplied phrase into a valid {@code tsquery} expression before
	 * calling this method.
	 * <p>
	 * When {@code term} is blank, this same executor is returned unchanged: no condition or ranking sort
	 * is applied.
	 *
	 * @param term the search term, already valid {@code tsquery} syntax, can be {@literal null} or blank
	 * @param field the {@code tsvector} field to match and rank against, can't be {@literal null}
	 * @param order the sort order applied to the {@code ts_rank} value, can't be {@literal null}
	 * @return a ranked {@link PageableExecutor}, or this same executor if {@code term} is blank, never {@literal null}.
	 */
	public PageableExecutor rankBy(@Nullable String term, Field<?> field, SortOrder order) {
		if (StringUtils.hasText(term)) {
			return new RankedPageableExecutor(
					DSL.condition("{0} @@ to_tsquery('simple', {1})", field, term),
					DSL.field("ts_rank({0}, to_tsquery('simple', {1}))", Double.class, field, term).sort(order),
					sortFields,
					defaultSortField
			);
		}

		return this;
	}

	/**
	 * Applies the {@link Pageable} instructions and executes the query built by {@code querySupplier} to
	 * construct a {@link Page}, with no additional filtering condition beyond whatever {@code querySupplier}
	 * already builds in. Equivalent to {@link #execute(Supplier, Supplier, Converter, Pageable)} with a
	 * condition supplier that always returns {@literal null}.
	 *
	 * @param querySupplier supplies the base query to execute; called once for the content fetch and once
	 *        more, independently, to build the total row count, must not be {@literal null}.
	 * @param mapper result set mapper applied on the retrieved record result set, must not be {@literal null}.
	 * @param pageable must not be {@literal null} but can be {@link Pageable#unpaged()}.
	 * @param <R> record type produced by the query returned from {@code querySupplier}
	 * @param <Q> query type returned from {@code querySupplier}
	 * @param <T> page content type that would be returned from the record converter
	 * @return the {@link Page} for the retrieved and converted result set and a total size, never {@literal null}.
	 */
	public <R extends Record, Q extends SelectWhereStep<R>, T> Page<T> execute(
			Supplier<Q> querySupplier,
			Converter<R, T> mapper,
			Pageable pageable
	) {
		return execute(querySupplier, () -> null, mapper, pageable);
	}

	/**
	 * Applies the {@link Pageable} instructions and the {@code condition} supplied by {@code conditionSupplier}
	 * to the query built by {@code querySupplier}, executing it to construct a {@link Page} of the mapped
	 * result set and its total size, applying optimizations via {@link PageableExecutionUtils}.
	 * <p>
	 * {@code querySupplier} is invoked twice: once to fetch the page content (with sorting, offset and limit
	 * applied), and once more, independently, to compute the total row count via
	 * {@code DSL.using(query.configuration()).fetchCountLarge(...)} — both invocations get the same
	 * {@code condition}, so the two stay consistent even when this executor was derived via
	 * {@link #rankBy(String, Field, SortOrder)}, which augments {@code condition} with its own match clause.
	 *
	 * @param querySupplier supplies the base query to execute; called once for the content fetch and once
	 *        more, independently, to build the total row count, must not be {@literal null}.
	 * @param conditionSupplier supplies the filtering condition applied to both the content and count
	 *        queries; may return {@literal null}, in which case no filtering condition is applied beyond
	 *        whatever {@code querySupplier} already builds in, must not be {@literal null}.
	 * @param mapper result set mapper applied on the retrieved record result set, must not be {@literal null}.
	 * @param pageable must not be {@literal null} but can be {@link Pageable#unpaged()}.
	 * @param <R> record type produced by the query returned from {@code querySupplier}
	 * @param <Q> query type returned from {@code querySupplier}
	 * @param <T> page content type that would be returned from the record converter
	 * @return the {@link Page} for the retrieved and converted result set and a total size, never {@literal null}.
	 */
	public <R extends Record, Q extends SelectWhereStep<R>, T> Page<T> execute(
			Supplier<Q> querySupplier,
			Supplier<@Nullable Condition> conditionSupplier,
			Converter<R, T> mapper,
			Pageable pageable
	) {
		final Condition condition = createCondition(conditionSupplier.get());
		final List<T> results = apply(querySupplier.get(), condition, pageable)
				.fetch(mapper::convert);

		return PageableExecutionUtils.getPage(results, pageable, () -> count(querySupplier.get(), condition));
	}

	private <R extends Record> ResultQuery<R> apply(SelectWhereStep<R> query, Condition condition, Pageable pageable) {
		final Collection<OrderField<?>> orderBy = createOrderBy(pageable.getSort());

		if (pageable.isUnpaged()) {
			return query.where(condition)
					.orderBy(orderBy);
		}

		return query.where(condition)
				.orderBy(orderBy)
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize());
	}

	private <R extends Record> Long count(SelectWhereStep<R> query, Condition condition) {
		final DSLContext context = DSL.using(query.configuration());
		return context.fetchCountLarge(query.where(condition));
	}

	protected Condition createCondition(@Nullable Condition condition) {
		return condition == null ? DSL.noCondition() : condition;
	}

	protected Collection<OrderField<?>> createOrderBy(@Nullable Sort sort) {
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
		private @Nullable OrderField<?> defaultSortField;

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

	private static final class RankedPageableExecutor extends PageableExecutor {

		private final Condition rankedCondition;
		private final OrderField<Double> rankedSort;

		private RankedPageableExecutor(
				Condition rankedCondition,
				OrderField<Double> rankedSort,
				Map<String, Field<?>> sortFields,
				OrderField<?> defaultSortField
		) {
			super(sortFields, defaultSortField);
			this.rankedCondition = rankedCondition;
			this.rankedSort = rankedSort;
		}

		@Override
		protected Condition createCondition(@Nullable Condition condition) {
			return condition == null ? rankedCondition : condition.and(rankedCondition);
		}

		@Override
		protected Collection<OrderField<?>> createOrderBy(@Nullable Sort sort) {
			final List<OrderField<?>> sorts = new ArrayList<>();
			sorts.add(rankedSort);
			sorts.addAll(super.createOrderBy(sort));
			return sorts;
		}
	}
}
