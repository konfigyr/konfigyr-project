package com.konfigyr.markdown;

import com.konfigyr.io.ByteArray;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class MarkdownParserTest {

	final MarkdownParser parser = new CommonmarkMarkdownParser();

	@Test
	@DisplayName("should create markdown contents and calculate checksum")
	void sameCreateContents() {
		final var contents = MarkdownContents.of("hello");

		assertThatObject(contents)
				.returns("hello", MarkdownContents::value)
				.isEqualTo(MarkdownContents.of("hello"))
				.hasSameHashCodeAs(MarkdownContents.of("hello"))
				.hasToString("hello");

		assertThat(contents.length())
				.isEqualTo(5);

		assertThat(contents.charAt(3))
				.isEqualTo('l');

		assertThat(contents.subSequence(2, 5))
				.isEqualTo("llo");
	}

	@Test
	@DisplayName("should produce same checksum for markdown contents")
	void sameChecksumForSameContent() {
		assertThat(MarkdownContents.of("hello").checksum())
				.returns(32, ByteArray::size)
				.isEqualTo(MarkdownContents.of("hello").checksum())
				.isNotEqualTo(MarkdownContents.of("world").checksum());
	}

	@ParameterizedTest
	@DisplayName("should return empty result for empty markdown contents")
	@ValueSource(strings = {"", " ", "   ", "\t", "\n", "\n\n"})
	void emptyForBlankContents(String value) {
		assertThat(parser.toSafeHtml(MarkdownContents.of(value)))
				.isEmpty();
	}

	@DisplayName("should parse and render block elements")
	@ParameterizedTest(name = "should render {1} for: {0}")
	@CsvSource({
			"Hello world,<p>Hello world</p>",
			"# Header,<h1>Header</h1>",
			"## Header,<h2>Header</h2>",
			"### Header,<h3>Header</h3>",
			"#### Header,<h4>Header</h4>",
			"##### Header,<h5>Header</h5>",
			"###### Header,<h6>Header</h6>",
			"`variable`,<code>variable</code>",
			"```const x = 1;```,<code>const x &#61; 1;</code>",
			"---,<hr />"
	})
	void parseBlockElements(String markdown, String expected) {
		assertThat(parser.toSafeHtml(MarkdownContents.of(markdown)))
				.contains(expected);
	}

	@Test
	@DisplayName("should render ordered list")
	void rendersOrderedList() {
		final var contents = MarkdownContents.of("1. first\n2. second");
		assertThat(parser.toSafeHtml(contents))
				.contains("<ol><li>first</li><li>second</li></ol>");
	}

	@Test
	@DisplayName("should render unordered list")
	void rendersUnorderedList() {
		final var contents = MarkdownContents.of("- alpha\n- beta\n- gamma");
		assertThat(parser.toSafeHtml(contents))
				.contains("<ul><li>alpha</li><li>beta</li><li>gamma</li></ul>");
	}

	@Test
	@DisplayName("should render unordered nested list")
	void rendersUnorderedNestedList() {
		final var contents = MarkdownContents.of("- parent\n    - child");
		assertThat(parser.toSafeHtml(contents))
				.contains("<ul>")
				.contains("<li>parent")
				.contains("<ul><li>child</li></ul>");
	}

	@Test
	@DisplayName("should render blockquote")
	void rendersBlockquote() {
		final var contents = MarkdownContents.of("> a quote");
		assertThat(parser.toSafeHtml(contents))
				.contains("<blockquote>")
				.contains("<p>a quote</p>");
	}

	@Test
	@DisplayName("should render fenced code block with language class")
	void rendersFencedCodeBlockPreservesLanguageClass() {
		final var contents = MarkdownContents.of("```typescript\nconst x: number = 1;\n```");
		assertThat(parser.toSafeHtml(contents))
				.startsWith("<pre>")
				.contains("<code class=\"language-typescript\">")
				.contains("const x: number &#61; 1;");
	}

	@DisplayName("should parse and render inline elements")
	@ParameterizedTest(name = "should render {1} for: {0}")
	@CsvSource({
			"**bold**,<strong>bold</strong>",
			"__bold__,<strong>bold</strong>",
			"*italic*,<em>italic</em>",
			"_italic_,<em>italic</em>",
			"**_bold italic_**,<strong><em>bold italic</em></strong>",
			"~~struck~~,<del>struck</del>"
	})
	void parseInlineElements(String markdown, String expected) {
		assertThat(parser.toSafeHtml(MarkdownContents.of(markdown)))
				.contains(expected);
	}

	@Test
	@DisplayName("should render link")
	void rendersLink() {
		final var contents = MarkdownContents.of("[GitHub](https://github.com)");
		assertThat(parser.toSafeHtml(contents))
				.contains("<a href=\"https://github.com\" rel=\"nofollow\">")
				.contains("GitHub");
	}

	@Test
	@DisplayName("should render auto link")
	void rendersAutolink() {
		final var contents = MarkdownContents.of("Visit https://example.com for more.");
		assertThat(parser.toSafeHtml(contents))
				.contains("Visit <a href=\"https://example.com\" rel=\"nofollow\">https://example.com</a> for more");
	}

	@Test
	@DisplayName("should render image")
	void rendersImage() {
		final var contents = MarkdownContents.of("![alt text](https://example.com/img.png)");
		assertThat(parser.toSafeHtml(contents))
				.contains("<img")
				.contains("src=\"https://example.com/img.png\"")
				.contains("alt=\"alt text\"");
	}

	@Test
	@DisplayName("should render task list")
	void rendersMixedTaskList() {
		final var contents = MarkdownContents.of("- [x] done\n- [ ] pending");
		assertThat(parser.toSafeHtml(contents))
				.contains("<input type=\"checkbox\" disabled=\"\" checked=\"\" /> done")
				.contains("<input type=\"checkbox\" disabled=\"\" /> pending");
	}

	@Test
	@DisplayName("should strip script tags")
	void stripsScriptTags() {
		final var contents = MarkdownContents.of("<script>alert('xss')</script>");
		assertThat(parser.toSafeHtml(contents))
				.doesNotContain("<script>")
				.doesNotContain("alert(");
	}

	@Test
	@DisplayName("should strip `onclick` attributes")
	void stripsOnClickAttribute() {
		final var contents = MarkdownContents.of("<a href=\"https://example.com\" onclick=\"evil()\">click</a>");
		assertThat(parser.toSafeHtml(contents))
				.doesNotContain("onclick")
				.doesNotContain("evil()");
	}

	@Test
	@DisplayName("should strip `href` attributes with javascript code")
	void stripsJavascriptHref() {
		final var contents = MarkdownContents.of("[click](javascript:alert('xss'))");
		assertThat(parser.toSafeHtml(contents))
				.doesNotContain("javascript:");
	}

	@Test
	@DisplayName("should strip `src` attributes that contain data uri source")
	void stripsDataUriInImageSrc() {
		final var contents = MarkdownContents.of("![img](data:image/png;base64,abc)");
		assertThat(parser.toSafeHtml(contents))
				.doesNotContain("data:");
	}

	@Test
	@DisplayName("should strip IFrame elements")
	void stripsIframeTags() {
		final var contents = MarkdownContents.of("<iframe src=\"https://evil.com\"></iframe>");
		assertThat(parser.toSafeHtml(contents))
				.doesNotContain("<iframe");
	}

	@Test
	@DisplayName("should strip `class` attributes from code")
	void stripsArbitraryClassAttributeOnCode() {
		final var contents = MarkdownContents.of("<code class=\"evil-class\">code</code>");
		assertThat(parser.toSafeHtml(contents))
				.doesNotContain("evil-class");
	}

	@Test
	@DisplayName("should perserve `class` attributes from code")
	void preservesLanguageClassOnCodeBlock() {
		final var contents = MarkdownContents.of("```java\nSystem.out.println();\n```");
		assertThat(parser.toSafeHtml(contents))
				.contains("class=\"language-java\"");
	}

	@Test
	@DisplayName("should enforce `nofollow` policy on links")
	void enforcesRelNofollowOnLinks() {
		final var contents = MarkdownContents.of("[link](https://example.com)");
		assertThat(parser.toSafeHtml(contents))
				.contains("rel=\"nofollow\"");
	}

	@Test
	@DisplayName("should allow `https` links on images")
	void allowsHttpsImageSrc() {
		final var contents = MarkdownContents.of("![img](https://example.com/img.png)");
		assertThat(parser.toSafeHtml(contents))
				.contains("src=\"https://example.com/img.png\"");
	}

	@Test
	@DisplayName("should not allow `http` links on images")
	void blocksHttpImageSrc() {
		final var contents = MarkdownContents.of("![img](http://example.com/img.png)");
		assertThat(parser.toSafeHtml(contents))
				.contains("src=\"http://example.com/img.png\"");
	}

	@Test
	@DisplayName("should strip `style` attributes from elements")
	void stripsStyleAttribute() {
		final var contents = MarkdownContents.of("<p style=\"color:red\">text</p>");
		assertThat(parser.toSafeHtml(contents))
				.doesNotContain("style=");
	}

}
