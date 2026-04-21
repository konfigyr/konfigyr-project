package com.konfigyr.markdown;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Jackson module that registers (de)serializers for {@link MarkdownContents}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
class MarkdownModule extends SimpleModule {

	static final String MARKDOWN_FIELD = "markdown";
	static final String HTML_FIELD = "html";

	MarkdownModule(MarkdownParser parser) {
		super("markdown-module");
		addSerializer(MarkdownContents.class, new MarkdownContentsSerializer(parser));
		addDeserializer(MarkdownContents.class, new MarkdownContentsDeserializer());
	}

	/**
	 * Serializes {@link MarkdownContents} to a JSON object:
	 *
	 * <pre>{@code
	 * {
	 *   "markdown": "# Hello",
	 *   "html": "<h1>Hello</h1>"
	 * }
	 * }</pre>
	 *
	 * The HTML is derived by invoking the {@link MarkdownParser} at serialization time.
	 * The parser is responsible for caching — the serializer never caches directly.
	 */
	static class MarkdownContentsSerializer extends StdSerializer<MarkdownContents> {

		private final MarkdownParser parser;

		MarkdownContentsSerializer(MarkdownParser parser) {
			super(MarkdownContents.class);
			this.parser = parser;
		}

		@Override
		public void serialize(MarkdownContents contents, JsonGenerator gen, SerializationContext ctx) {
			gen.writeStartObject();
			gen.writeStringProperty(MARKDOWN_FIELD, contents.value());
			gen.writeStringProperty(HTML_FIELD, parser.toSafeHtml(contents));
			gen.writeEndObject();
		}
	}

	/**
	 * Deserializes a plain JSON string into a {@link MarkdownContents}.
	 * JSON objects are explicitly rejected — the HTML field is never trusted from input.
	 *
	 * <pre>{@code
	 * // Accepted:
	 * "# Hello world"
	 *
	 * // Rejected:
	 * { "markdown": "# Hello", "html": "<h1>Hello</h1>" }
	 * }</pre>
	 */
	static class MarkdownContentsDeserializer extends StdDeserializer<MarkdownContents> {

		MarkdownContentsDeserializer() {
			super(MarkdownContents.class);
		}

		@Override
		public MarkdownContents deserialize(JsonParser parser, DeserializationContext ctx) throws JacksonException {
			if (parser.currentToken() == JsonToken.VALUE_STRING) {
				return MarkdownContents.of(parser.getValueAsString());
			}

			if (parser.currentToken() == JsonToken.START_OBJECT) {
				String markdown = null;
				while (parser.nextToken() != JsonToken.END_OBJECT) {
					if (MARKDOWN_FIELD.equals(parser.currentName())) {
						parser.nextToken();
						markdown = parser.getString();
					} else {
						parser.nextToken();
						parser.skipChildren();
					}
				}

				if (markdown != null) {
					return MarkdownContents.of(markdown);
				}
			}

			throw MismatchedInputException.from(parser, MarkdownContents.class,
					"MarkdownContents can only be deserialized from a JSON string. Got: "
							+ parser.currentToken());
		}
	}

}
