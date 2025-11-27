package com.konfigyr.artifactory.store;

import com.konfigyr.artifactory.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.json.JacksonJsonObjectReader;
import org.springframework.batch.infrastructure.item.json.JsonItemReader;
import org.springframework.batch.infrastructure.item.json.JsonObjectReader;
import org.springframework.core.io.Resource;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.io.Serial;
import java.util.List;

/**
 * Implementation of the {@link org.springframework.batch.infrastructure.item.ItemReader} that would read the
 * {@link PropertyMetadata} from the uploaded Spring Boot configuration metadata file.
 * <p>
 * It is important to note that this reader implementation should be defined as a Spring Bean with a <code>step</code>
 * scope, via {@link org.springframework.batch.core.configuration.annotation.StepScope} annotation, as this reader
 * uses the {@link JsonItemReader} that is not thread-safe.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see JsonItemReader
 */
public class MetadataStoreReader implements ItemStreamReader<@NonNull PropertyMetadata> {

	private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
			.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
			.disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
			.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
			.build();

	private final JsonItemReader<@NonNull PropertyMetadata> delegate;

	public MetadataStoreReader(final String coordinates, final MetadataStore store) {
		this(ArtifactCoordinates.parse(coordinates), store);
	}

	public MetadataStoreReader(final ArtifactCoordinates coordinates, final MetadataStore store) {
		this.delegate = createItemReader(coordinates, store);
	}

	@Override
	public void open(@NonNull ExecutionContext context) throws ItemStreamException {
		System.out.println("Opening reader: " + context.toMap());
		System.out.println("Opening reader: " + delegate.getCurrentItemCount());
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

	static JsonItemReader<@NonNull PropertyMetadata> createItemReader(
			final ArtifactCoordinates coordinates,
			final MetadataStore store
	) {
		final Resource resource = store.get(coordinates).orElseThrow(() -> new IllegalStateException(
				"Could not find uploaded artifact property metadata for coordinates: " + coordinates.format()
		));

		final JsonObjectReader<@NonNull PropertyMetadata> reader = new JacksonJsonObjectReader<>(
				JSON_MAPPER, UploadedPropertyMetadata.class
		);

		return new JsonItemReader<>(resource, reader);
	}

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
