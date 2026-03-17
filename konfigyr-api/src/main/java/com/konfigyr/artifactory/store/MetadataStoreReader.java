package com.konfigyr.artifactory.store;

import com.konfigyr.artifactory.*;
import org.jspecify.annotations.NonNull;
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

/**
 * Implementation of the {@link org.springframework.batch.infrastructure.item.ItemReader} that would read the
 * {@link PropertyDescriptor property descriptors} from the uploaded Spring Boot configuration metadata file.
 * <p>
 * It is important to note that this reader implementation should be defined as a Spring Bean with a <code>step</code>
 * scope, via {@link org.springframework.batch.core.configuration.annotation.StepScope} annotation, as this reader
 * uses the {@link JsonItemReader} that is not thread-safe.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see JsonItemReader
 */
public class MetadataStoreReader implements ItemStreamReader<@NonNull PropertyDescriptor> {

	private final JsonItemReader<@NonNull PropertyDescriptor> delegate;

	public MetadataStoreReader(final String coordinates, final JsonMapper jsonMapper, final MetadataStore store) {
		this(ArtifactCoordinates.parse(coordinates), jsonMapper, store);
	}

	public MetadataStoreReader(final ArtifactCoordinates coordinates, final JsonMapper jsonMapper, final MetadataStore store) {
		this.delegate = createItemReader(coordinates, jsonMapper, store);
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
	public PropertyDescriptor read() throws Exception {
		return this.delegate.read();
	}

	@Override
	public void close() throws ItemStreamException {
		this.delegate.close();
	}

	static JsonItemReader<@NonNull PropertyDescriptor> createItemReader(
			final ArtifactCoordinates coordinates,
			final JsonMapper jsonMapper,
			final MetadataStore store
	) {
		final Resource resource = store.get(coordinates).orElseThrow(() -> new IllegalStateException(
				"Could not find uploaded artifact property metadata for coordinates: " + coordinates.format()
		));

		final JsonMapper customizedJsonMapper = jsonMapper.rebuild()
				.propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
				.disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
				.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
				.build();

		final JsonObjectReader<@NonNull PropertyDescriptor> reader = new JacksonJsonObjectReader<>(
				customizedJsonMapper, DefaultPropertyDescriptor.class
		);

		return new JsonItemReader<>(resource, reader);
	}

}
