package com.konfigyr.artifactory.store;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.konfigyr.artifactory.*;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.JsonObjectReader;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.Serial;
import java.util.List;

/**
 * Implementation of the {@link org.springframework.batch.item.ItemReader} that would read the {@link PropertyMetadata}
 * from the uploaded Spring Boot configuration metadata file.
 * <p>
 * It is important to note that this reader implementation should be defined as a Spring Bean with a <code>step</code>
 * scope, via {@link org.springframework.batch.core.configuration.annotation.StepScope} annotation, as this reader
 * uses the {@link JsonItemReader} that is not thread-safe.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see JsonItemReader
 */
public class MetadataStoreReader implements ItemStreamReader<PropertyMetadata> {

	private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
			.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
			.build();

	private final JsonItemReader<PropertyMetadata> delegate;

	public MetadataStoreReader(final String coordinates, final MetadataStore store) {
		this(ArtifactCoordinates.parse(coordinates), store);
	}

	public MetadataStoreReader(final ArtifactCoordinates coordinates, final MetadataStore store) {
		this.delegate = createItemReader(coordinates, store);
	}

	@Override
	public void open(@NonNull ExecutionContext context) throws ItemStreamException {
		this.delegate.open(context);
	}

	@Override
	public void update(@NonNull ExecutionContext context) throws ItemStreamException {
		this.delegate.update(context);
	}

	@Override
	public PropertyMetadata read() throws Exception {
		return this.delegate.read();
	}

	@Override
	public void close() throws ItemStreamException {
		this.delegate.close();
	}

	static JsonItemReader<PropertyMetadata> createItemReader(
			final ArtifactCoordinates coordinates,
			final MetadataStore store
	) {
		final Resource resource = store.get(coordinates).orElseThrow(() -> new IllegalStateException(
				"Could not find uploaded artifact property metadata for coordinates: " + coordinates.format()
		));

		final JsonObjectReader<PropertyMetadata> reader = new JacksonJsonObjectReader<>(
				JSON_MAPPER, UploadedPropertyMetadata.class
		);

		return new JsonItemReader<>(resource, reader);
	}

	@Builder
	@Jacksonized
	record UploadedPropertyMetadata(
			@NonNull String name,
			@NonNull DataType dataType,
			@NonNull PropertyType type,
			@NonNull String typeName,
			@Nullable String defaultValue,
			@Nullable String description,
			@NonNull List<String> hints,
			@NonNull Deprecation deprecation
	) implements PropertyMetadata {

		@Serial
		private static final long serialVersionUID = 3702255252059270913L;

	}

}
