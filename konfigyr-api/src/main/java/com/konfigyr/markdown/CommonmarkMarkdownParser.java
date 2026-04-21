package com.konfigyr.markdown;

import org.apache.commons.lang3.StringUtils;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jspecify.annotations.NullMarked;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

/**
 * {@link MarkdownParser} implementation that parses Markdown to sanitized HTML.
 * <p>
 * This implementation uses:
 * <ul>
 *     <li>
 *         <a href="https://github.com/commonmark/commonmark-java">commonmark-java</a>
 *         library for CommonMark-compliant parsing and rendering.
 *     </li>
 *     <li>
 *         <a href="https://github.com/OWASP/java-html-sanitizer">OWASP Java HTML Sanitizer</a>
 *         for allowlist-based HTML sanitization.
 *     </li>
 * </ul>
 * <p>
 * The rendering pipeline runs in three steps: {@code commonmark-java} parses the raw Markdown
 * string into an AST, renders that AST to an HTML string, and then OWASP strips any tags,
 * attributes, or URL schemes not explicitly permitted by the policy. The sanitizer always runs
 * last, no transformations are applied after it.
 * <p>
 * Rendered output is cached by Spring using {@link MarkdownContents#checksum()} as the key,
 * so identical content is only rendered once regardless of how many callers request it. The
 * underlying cache must be declared in the application context
 * <p>
 * The {@code commonmark-java} {@link Parser} and {@link HtmlRenderer} are immutable after
 * construction, and the OWASP {@link PolicyFactory} is likewise thread-safe, so this bean is safe
 * to use as a singleton under a concurrent load.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
class CommonmarkMarkdownParser implements MarkdownParser {

	private final Parser parser;
	private final HtmlRenderer renderer;
	private final PolicyFactory sanitizer;

	CommonmarkMarkdownParser() {
		final List<Extension> extensions = List.of(
				StrikethroughExtension.create(),
				TaskListItemsExtension.create(),
				AutolinkExtension.create()
		);

		this.parser = Parser.builder()
				.extensions(extensions)
				.build();

		this.renderer = HtmlRenderer.builder()
				.extensions(extensions)
				.build();

		this.sanitizer = buildSanitizer();
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The result is cached by Spring under the {@code markdown-html} cache using
	 * {@link MarkdownContents#checksum()} as the key, so the full parse–render–sanitize
	 * pipeline only runs once per unique document regardless of concurrent callers.
	 * An empty string is returned immediately for blank content without entering the pipeline.
	 */
	@Override
	@Cacheable(value = "markdown-parser", key = "#contents.checksum()")
	public String toSafeHtml(MarkdownContents contents) {
		if (StringUtils.isBlank(contents.value())) {
			return StringUtils.EMPTY;
		}

		final Node document = parser.parse(contents.value());
		final String html = renderer.render(document);
		return sanitizer.sanitize(html);
	}

	/**
	 * Builds the OWASP HTML sanitizer policy used by this parser.
	 *
	 * <p>The allowlist is modeled on GitHub's GFM content policy. Standard block
	 * and inline elements produced by commonmark-java are permitted. Link {@code href}
	 * attributes are restricted to {@code http}, {@code https}, and {@code mailto}
	 * schemes, blocking {@code javascript:} URIs entirely, and {@code rel="nofollow"}
	 * is enforced on all anchors. Image {@code src} attributes are restricted to
	 * {@code http} and {@code https}. Task list {@code <input>} elements are allowed
	 * only with {@code type="checkbox"} and the {@code checked} and {@code disabled}
	 * attributes, which is exactly what commonmark-java emits for task list items.
	 * {@code class} attributes on {@code <code>} and {@code <pre>} elements are
	 * permitted only when the value starts with {@code language-}, preserving syntax
	 * highlighting markers while blocking arbitrary class injection.
	 *
	 * @return a configured, immutable, thread-safe {@link PolicyFactory}
	 */
	private static PolicyFactory buildSanitizer() {
		return new HtmlPolicyBuilder()
				// Block elements
				.allowElements(
						"p", "br", "hr", "blockquote", "pre",
						"ul", "ol", "li",
						"h1", "h2", "h3", "h4", "h5", "h6"
				)
				// Inline elements
				.allowElements(
						"strong", "em", "del", "code", "tt"
				)
				// Links — href restricted to safe schemes
				.allowElements("a")
				.allowAttributes("href").onElements("a")
				.allowStandardUrlProtocols()
				.requireRelNofollowOnLinks()
				// Images — src restricted to http/https only
				.allowElements("img")
				.allowAttributes("src", "alt", "title").onElements("img")
				.allowUrlProtocols("https", "http")
				// Tables (GFM)
				.allowElements("table", "thead", "tbody", "tfoot", "tr", "th", "td")
				.allowAttributes("align").onElements("th", "td")
				// Task list checkboxes — always rendered as disabled by commonmark-java
				.allowElements("input")
				.allowAttributes("type").onElements("input")
				.allowAttributes("checked", "disabled").onElements("input")
				// Syntax highlighting — allow only language-like classes on code/pre
				.allowAttributes("class")
				.matching(className -> className.startsWith("language-"))
				.onElements("code", "pre")
				.toFactory();
	}

}
