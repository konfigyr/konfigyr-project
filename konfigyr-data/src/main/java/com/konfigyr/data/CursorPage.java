package com.konfigyr.data;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.util.Streamable;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A container for a page of data retrieved via cursor-based pagination.
 * <p>
 * This record encapsulates the result set along with the metadata required for the client to
 * navigate to the next or previous page, or slice, of results.
 * <p>
 * Unlike offset-based pagination, this response does not provide a <i>total page count</i>
 * or <i>current page number</i>, as those values are computationally expensive and inherently
 * unstable in highly dynamic datasets like history or audit logs.
 *
 * @param <T> The type of the items contained in this page.
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see CursorPageable
 */
@NullMarked
public interface CursorPage<T> extends Streamable<T> {

	/**
	 * Creates an empty {@link CursorPage} representing a result set with no data.
	 * <p>
	 * This is typically used as a fallback when a search query yields no matches or when the end of a
	 * chronological timeline has been reached.
	 *
	 * @param <T> the expected type of the page content.
	 * @return an empty page instance.
	 */
	static <T> CursorPage<T> empty() {
		return of(Collections.emptyList(), null);
	}

	/**
	 * Creates an empty {@link CursorPage} bound to specific pagination metadata.
	 * <p>
	 * This is useful for returning an empty result set while still providing the client with the context of
	 * the requested page size or a potential <i>next</i> starting point.
	 *
	 * @param <T>          the expected type of the page content.
	 * @param nextPageable the pagination metadata to associate with this empty page. Can be {@literal null}.
	 * @return an empty {@link CursorPage} wrapping the provided pagination state.
	 */
	static <T> CursorPage<T> empty(CursorPageable nextPageable) {
		return of(Collections.emptyList(), nextPageable);
	}

	/**
	 * Creates a {@link CursorPage} containing a full result set.
	 * <p>
	 * Use this factory method when the no <i>next</i> or <i>previous</i> navigation is not possible or
	 * not needed. The provided {@code contents} list is wrapped in an unmodifiable view to ensure the
	 * integrity of the page.
	 *
	 * @param <T>          the type of the items in the page.
	 * @param contents     the list of records fetched from the store, can't be {@literal null}.
	 * @return a populated {@link CursorPage} without navigation metadata.
	 */
	static <T> CursorPage<T> of(List<T> contents) {
		return new DefaultCursorPage<>(Collections.unmodifiableList(contents), null, null);
	}

	/**
	 * Creates a {@link CursorPage} containing a forward-only result set.
	 * <p>
	 * Use this factory method when only <i>next</i> navigation is required (e.g., simple infinite scrolling).
	 * The provided {@code contents} list is wrapped in an unmodifiable view to ensure the integrity of the page.
	 *
	 * @param <T>          the type of the items in the page.
	 * @param contents     the list of records fetched from the store, can't be {@literal null}.
	 * @param nextPageable the metadata required to fetch the next page, or {@code null} if this is the last page.
	 * @return a populated {@link CursorPage} with forward-navigation metadata.
	 */
	static <T> CursorPage<T> of(List<T> contents, @Nullable CursorPageable nextPageable) {
		return new DefaultCursorPage<>(Collections.unmodifiableList(contents), nextPageable, null);
	}

	/**
	 * Creates a {@link CursorPage} containing a result set with full bidirectional navigation metadata.
	 * <p>
	 * This is the primary factory method for creating the full cursor page page, providing the current state
	 * of the timeline along with <i>bookmarks</i> to navigate both forward to newer events and backward to
	 * historical states.
	 *
	 * @param <T>              the type of the items in the page.
	 * @param contents     	   the list of records fetched from the store, can't be {@literal null}.
	 * @param nextPageable     the metadata for the next page, or {@code null}.
	 * @param previousPageable The metadata for the preceding page, or {@code null}.
	 * @return A fully-formed {@link CursorPage} with complete navigation context.
	 */
	static <T> CursorPage<T> of(List<T> contents, @Nullable CursorPageable nextPageable, @Nullable CursorPageable previousPageable) {
		return new DefaultCursorPage<>(Collections.unmodifiableList(contents), nextPageable, previousPageable);
	}

	/**
	 * Returns the list of items retrieved for the current request.
	 *
	 * @return the page contents, never {@code null}.
	 */
	List<T> content();

	/**
	 * Returns the number of items currently held in this page's content.
	 *
	 * @return the size of the {@link #content()} list.
	 */
	default int size() {
		return content().size();
	}

	/**
	 * Returns the {@link CursorPageable} to request the next {@link CursorPage}. Can be {@code null}
	 * in case the current page is already the last one. Clients should check {@link #hasNext()} before
	 * calling this method.
	 *
	 * @return the {@link CursorPageable} to request the next page, or {@code null} if there are none.
	 */
	@Nullable
	CursorPageable nextPageable();

	/**
	 * Returns the {@link CursorPageable} to request the previous {@link CursorPage}. Can be {@code null}
	 * in case the current page is already the first one. Clients should check {@link #hasPrevious()}
	 * before calling this method.
	 *
	 * @return the {@link CursorPageable} to request the previous page, or {@code null} if it's the first one.
	 */
	@Nullable
	CursorPageable previousPageable();

	/**
	 * Indicates whether there is a next page of data available to be fetched.
	 *
	 * @return {@code true} if a <i>next</i> cursor can be used, {@code false} otherwise.
	 */
	boolean hasNext();

	/**
	 * Indicates whether there is a preceding page of data available.
	 *
	 * @return {@code true} if a <i>previous</i> cursor is available, {@code false} otherwise.
	 */
	boolean hasPrevious();

	@Override
	default <R> CursorPage<R> map(Function<? super T, ? extends R> mapper) {
		return of(stream().map(mapper).collect(Collectors.toUnmodifiableList()), nextPageable(), previousPageable());
	}
}
