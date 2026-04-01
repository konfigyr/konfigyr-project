package com.konfigyr.data;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;

/**
 * Interface that defines the contract for cursor-based pagination metadata.
 * <p>
 * Unlike traditional {@code offset-based} pagination (e.g., Spring Data's {@code Pageable}),
 * which uses {@code page} and {@code size} to skip rows, <strong>Cursor-based pagination</strong>
 * (also known as keyset pagination) uses an opaque {@code token} to point to a specific record in
 * a sorted result set.
 *
 * <h3>Why Cursor-based Pagination?</h3>
 *
 * <ul>
 *   <li>
 *       <b>Performance:</b> Offsets become increasingly slow as the dataset grows because
 *       the database must scan and discard all previous rows. Cursors allow the database
 *       to jump directly to the next set of results using an index.
 *   </li>
 *   <li>
 *       <b>Consistency:</b> Offsets are prone to <i>drifting</i> (skipping or duplicating items)
 *       if records are inserted or deleted between page requests. Cursors remain tied to
 *       specific data points, ensuring a stable <i>infinite scroll</i> experience.
 *   </li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see CursorPage
 */
@NullMarked
public interface CursorPageable extends Serializable {

	/**
	 * Returns a {@link CursorPageable} instance representing a request for the entire, unconstrained dataset.
	 *
	 * @return a unpageable cursor pageable instance.
	 */
	static CursorPageable unpaged() {
		return UnpagedCursorPageable.INSTANCE;
	}

	/**
	 * Creates a {@link CursorPageable} for an initial request where no previous cursor is available.
	 *
	 * @param size the maximum number of items to be returned, must be {@code >= 1}.
	 * @return pageable instance representing the first page of a result set.
	 */
	static CursorPageable of(int size) {
		return new DefaultCursorPageable(null, size);
	}

	/**
	 * Creates a {@link CursorPageable} using a continuation token from a previous response.
	 *
	 * @param token the opaque string representing the token from the previous page.
	 *              If {@code null}, this effectively requests the first page.
	 * @param size  the maximum number of items to be returned, must be {@code >= 1}.
	 * @return pageable instance representing a specific point in the result set.
	 */
	static CursorPageable of(@Nullable String token, int size) {
		return new DefaultCursorPageable(token, size);
	}

	/**
	 * Returns the maximum number of items to be returned in this single request.
	 * <p>
	 * This value is used to constrain the result set (e.g., via a SQL {@code LIMIT} clause).
	 *
	 * @return the requested page size.
	 * @throws UnsupportedOperationException if the instance is {@link #isUnpaged()}.
	 */
	int size();

	/**
	 * Returns the opaque continuation token used to locate the starting point of this page.
	 * <p>
	 * This token is typically an encoded string containing the sorting keys (e.g., an ID and a Timestamp)
	 * of the last element from the previous page. It should be treated as immutable and opaque by the client.
	 *
	 * @return the cursor token, or {@code null} if starting from the very first record.
	 * @throws UnsupportedOperationException if the instance is {@link #isUnpaged()}.
	 */
	@Nullable
	String token();

	/**
	 * Indicates if this instance contains valid pagination parameters (size and/or token).
	 *
	 * @return {@code true} if paged, {@code false} if the entire dataset is requested.
	 */
	default boolean isPaged() {
		return true;
	}

	/**
	 * Indicates if this instance represents a request for the entire, unconstrained dataset.
	 *
	 * @return {@code true} if no pagination limits are applied.
	 */
	default boolean isUnpaged() {
		return !isPaged();
	}

}
