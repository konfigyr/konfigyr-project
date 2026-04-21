package com.konfigyr.markdown;

import org.jspecify.annotations.NullMarked;

/**
 * Strategy interface for converting raw Markdown content to sanitized HTML.
 * <p>
 * Implementations must be pure with respect to content: the same {@link MarkdownContents}
 * must always produce the same HTML output. The returned HTML must be fully sanitized before
 * being returned, callers must not apply any further transformations to the result,
 * as doing so may reintroduce XSS vulnerabilities. Sanitization must always be the final
 * step in the rendering pipeline.
 * <p>
 * Implementations are expected to be thread-safe and suitable for use as application-scoped
 * singletons. Caching rendered output is left to the implementation; {@link MarkdownContents#checksum()}
 * is provided as a stable, inexpensive cache key for this purpose.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see MarkdownContents
 */
@NullMarked
public interface MarkdownParser {

	/**
	 * Converts the given Markdown content to sanitized HTML.
	 * <p>
	 * The returned string is safe for direct injection into a browser DOM. Callers must not apply
	 * any further transformation to the result.
	 *
	 * @param contents the raw Markdown content to render, can't be {@code null}
	 * @return a sanitized HTML string, never {@code null}, empty if {@code contents} is blank
	 */
	String toSafeHtml(MarkdownContents contents);

}
