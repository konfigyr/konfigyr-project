package com.konfigyr.namespace;

import com.konfigyr.artifactory.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.version.Version;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents the complete configuration catalog of a {@link Service}.
 * <p>
 * The service catalog is a materialized collection of {@link PropertyDescriptor property descriptors}
 * that define all configuration properties available to a particular {@link Service}. Each descriptor
 * originates from configuration metadata published by artifacts that are part of the service's
 * dependency graph.
 * <p>
 * The catalog is derived from the service's {@link com.konfigyr.artifactory.Manifest}. A manifest describes
 * the exact set of {@link com.konfigyr.artifactory.Artifact}s that participate in a service release. When
 * a service release is published, the platform resolves the configuration metadata of all artifacts declared
 * in the manifest and compiles them into a catalog.
 * <p>
 * Each artifact may contribute one or more configuration property definitions. These definitions
 * include information such as the property's name, type, JSON schema, default value, documentation,
 * and deprecation metadata. The catalog therefore represents the authoritative set of configuration
 * descriptors available to the service.
 * <p>
 * The {@code version} identifies the specific revision of the catalog and is typically derived from
 * the manifest or the service release that produced it. Consumers may use this value to determine
 * whether a catalog has changed and to implement caching or synchronization strategies.
 * <p>
 * <strong>Usage</strong>
 * The catalog is primarily used by:
 * <ul>
 *     <li>
 *         <b>Configuration management systems</b>, such as Vault integrations, which combine the
 *         catalog descriptors with the current configuration state to produce validated configuration
 *         views.
 *     </li>
 *     <li>
 *         <b>User interfaces</b>, which use the catalog to provide property discovery, autocomplete,
 *         and documentation for configuration properties available to the service.
 *     </li>
 * </ul>
 * <p>
 * Since the catalog represents the full set of descriptors for a service, clients may choose to
 * perform local filtering or search operations on the returned descriptors rather than issuing
 * repeated remote queries.</p>
 *
 * @param id the unique identifier of the catalog; never {@literal null}
 * @param service the service slug to which the catalog belongs; never {@literal null}
 * @param version the version of the catalog associated with the service release; never {@literal null}
 * @param properties the immutable list of property descriptors that compose the catalog; never {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@AggregateRoot
public record ServiceCatalog(
		@Identity EntityId id,
		Service service,
		String version,
		List<Property> properties
) implements Iterable<ServiceCatalog.Property>, Serializable {

	/**
	 * Attempts to resolve the {@link Property} with the matching property name.
	 *
	 * @param name the name of the property to find, can't be {@literal null}
	 * @return the matching property, or {@code empty}, never {@literal null}
	 */
	public Optional<Property> get(String name) {
		return stream().filter(property -> property.name().equals(name)).findFirst();
	}

	public Stream<Property> stream() {
		return properties.stream();
	}

	@Override
	public Iterator<Property> iterator() {
		return properties.iterator();
	}

	/**
	 * Implementation of a {@link PropertyDescriptor} that describes a single configuration property
	 * within a service catalog. This property descriptor contains the artifact coordinates of the
	 * {@link Artifact} that owns the property.
	 *
	 * @param artifact the artifact coordinates of the artifact that owns the property; never {@literal null}
	 * @param name the name of the property; never {@literal null}
	 * @param schema the JSON schema describing the property's type and constraints; never {@literal null}
	 * @param typeName the fully qualified Java type name of the property's value; may be {@literal null}
	 * @param description human-readable description of the property purpose; may be {@literal null}
	 * @param defaultValue the default value of the property; may be {@literal null}
	 * @param deprecation deprecation metadata; may be {@literal null}
	 */
	@NullMarked
	@ValueObject
	public record Property(
			ArtifactCoordinates artifact,
			String name,
			JsonSchema schema,
			@Nullable String typeName,
			@Nullable String description,
			@Nullable String defaultValue,
			@Nullable Deprecation deprecation
	) implements PropertyDescriptor {

		public static PropertyBuilder builder() {
			return new PropertyBuilder();
		}

	}

	public static final class PropertyBuilder extends PropertyDescriptorBuilder<Property, PropertyBuilder> {

		private @Nullable String groupId;
		private @Nullable String artifactId;
		private @Nullable String version;

		private PropertyBuilder() {
			// use the static builder method
		}

		public PropertyBuilder coordinates(@Nullable ArtifactCoordinates coordinates) {
			if (coordinates == null) {
				return this;
			}
			return groupId(coordinates.groupId())
					.artifactId(coordinates.artifactId())
					.version(coordinates.version());
		}

		public PropertyBuilder groupId(@Nullable String groupId) {
			this.groupId = groupId;
			return this;
		}

		public PropertyBuilder artifactId(@Nullable String artifactId) {
			this.artifactId = artifactId;
			return this;
		}

		public PropertyBuilder version(@Nullable Version version) {
			return version(version == null ? null : version.get());
		}

		public PropertyBuilder version(@Nullable String version) {
			this.version = version;
			return this;
		}

		@Override
		public Property build() {
			Assert.hasText(name, "Service catalog property must have a name");
			Assert.hasText(typeName, "Service catalog property must have a type name");

			return new Property(ArtifactCoordinates.of(groupId, artifactId, version), name, schema, typeName,
					description, defaultValue, deprecation);
		}
	}
}
