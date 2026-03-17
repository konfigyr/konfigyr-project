package com.konfigyr.artifactory.converter;

import com.konfigyr.artifactory.Deprecation;
import com.konfigyr.artifactory.JsonSchema;
import com.konfigyr.data.converter.JsonByteArrayConverter;
import com.konfigyr.io.ByteArray;
import org.jooq.Converter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;

@NullMarked
public final class ArtifactoryConverters {

	private final JsonMapper jsonMapper;
	private final Converter<ByteArray, Deprecation> deprecationConverter;
	private final Converter<ByteArray, JsonSchema> schemaConverter;
	private final Converter<String, URI> uriConverter;

	public ArtifactoryConverters(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
		this.deprecationConverter = JsonByteArrayConverter.create(jsonMapper, Deprecation.class);
		this.schemaConverter = JsonByteArrayConverter.create(jsonMapper, JsonSchema.class);
		this.uriConverter = Converter.ofNullable(String.class, URI.class, URI::create, URI::toString);
	}

	public JsonMapper mapper() {
		return jsonMapper;
	}

	public Converter<@Nullable ByteArray, @Nullable Deprecation> deprecation() {
		return deprecationConverter;
	}

	public Converter<@Nullable ByteArray, @Nullable JsonSchema> schema() {
		return schemaConverter;
	}

	public Converter<@Nullable String, @Nullable URI> uri() {
		return uriConverter;
	}
}
