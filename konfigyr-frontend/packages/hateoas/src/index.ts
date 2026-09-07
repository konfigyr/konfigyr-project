/**
 * Markdown content paired with its rendered HTML.
 */
export interface MarkdownContents {
  /** Raw markdown source. */
  markdown: string;
  /** HTML rendered from {@link markdown}. */
  html: string;
}

/**
 * Pagination parameters used when requesting a page of a {@link PageResponse}.
 */
export interface Pageable {
  /** Zero-based page number to request. */
  page?: number
  /** Maximum number of items per page. */
  size?: number
  /** Sort expression, e.g. `field,asc` or `field,desc`. */
  sort?: string
}

/**
 * REST envelope wrapping a collection of items.
 */
export interface CollectionResponse<T> {
  /** Items contained in this response. */
  data: Array<T>;
}

/**
 * A {@link CollectionResponse} paginated by page number.
 */
export interface PageResponse<T> extends CollectionResponse<T> {
  metadata: {
    /** Number of items per page. */
    size?: number | null;
    /** Zero-based index of the current page. */
    number?: number | null;
    /** Total number of items across all pages. */
    total?: number | null;
    /** Total number of pages. */
    pages?: number | null;
  }
}

/**
 * A {@link CollectionResponse} paginated by opaque cursor tokens.
 */
export interface CursorResponse<T> extends CollectionResponse<T> {
  metadata: {
    /** Number of items in this page. */
    size?: number | null;
    /** Cursor token for the next page, or `null` if there is none. */
    next?: string | null;
    /** Cursor token for the previous page, or `null` if there is none. */
    previous?: string | null;
  }
}
