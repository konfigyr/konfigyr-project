package com.konfigyr.artifactory;

import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.version.Version;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.util.*;

/**
 * Entity representing a configuration property definition extracted from Spring Boot configuration metadata sources.
 * <p>
 * A {@link PropertyDefinition} describes a single configuration key, including its type, default value, and
 * documentation metadata. It belongs to a specific {@link Artifact}, and multiple artifact versions
 * can reuse the same definition through a many-to-many association.
 * <p>
 * In DDD terms, this entity forms part of the {@link Artifact} aggregate but represents a reusable component
 * shared across artifact versions. It should remain immutable once created to ensure referential integrity
 * across deduplicated property sets.
 *
 * @param id entity identifier for the property definition, can't be {@literal null}.
 * @param artifact entity identifier of the artifact that owns the property, can't be {@literal null}.
 * @param checksum unique checksum identifying this specific configuration definition, can't be {@literal null}.
 * @param name name of the configuration property, for example {@code spring.datasource.url}. Can't be {@literal null}.
 * @param dataType the property data type, can't be {@literal null}.
 * @param type the logical property type identifier, can't be {@literal null}.
 * @param typeName fully qualified Java type name, for example {@code java.lang.String}. Can't be {@literal null}.
 * @param defaultValue the default property value, can be {@literal null}.
 * @param description human-readable description of the property purpose, may be {@literal null}.
 * @param deprecation deprecation metadata, may be {@literal null}.
 * @param hints value hints, describing allowed or example property values, can't be {@literal null}.
 * @param occurrences number of artifact versions that currently reference this property, can't be {@literal null}.
 * @param firstSeen the artifact version identifier where this property first appeared, can't be {@literal null}.
 * @param lastSeen the artifact version identifier where this property was last observed, can't be {@literal null}.
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Entity
public record PropertyDefinition(
		@NonNull @Identity EntityId id,
		@NonNull @Association(aggregateType = Artifact.class) EntityId artifact,
		@NonNull ByteArray checksum,
		@NonNull String name,
		@NonNull DataType dataType,
		@NonNull PropertyType type,
		@NonNull String typeName,
		@Nullable String defaultValue,
		@Nullable String description,
		@NonNull List<String> hints,
		@NonNull Deprecation deprecation,
		int occurrences,
		@NonNull Version firstSeen,
		@NonNull Version lastSeen
) implements PropertyMetadata {

	@Serial
	private static final long serialVersionUID = 3702255252059270913L;

	/**
	 * Creates a new {@code Builder}.
	 *
	 * @return a new property definition builder instance, never {@literal null}.
	 */
	@NonNull
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for creating immutable {@link PropertyDefinition} instances.
	 * <p>
	 * This builder simplifies the construction of {@link PropertyDefinition} records
	 * that contain a large number of parameters, providing a fluent API for setting
	 * required and optional attributes.
	 *
	 * @author Vladimir Spasic
	 * @since 1.0.0
	 */
	public static final class Builder {

		private EntityId id;
		private EntityId artifact;
		private ByteArray checksum;
		private String name;
		private DataType dataType;
		private PropertyType type;
		private String typeName;
		private String defaultValue;
		private String description;
		private final Set<String> hints;
		private Deprecation deprecation;
		private int occurrences = 1;
		private Version firstSeen;
		private Version lastSeen;

		private Builder() {
			this.hints = new LinkedHashSet<>();
		}

		/**
		 * Specify the internal {@link EntityId} for this property definition.
		 *
		 * @param id internal entity identifier for the property definition.
		 * @return property definition builder
		 */
		@NonNull
		public Builder id(Long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for this property definition.
		 *
		 * @param id external entity identifier for the property definition.
		 * @return property definition builder
		 */
		@NonNull
		public Builder id(String id) {
			return id(EntityId.from(id));
		}

		/**
		 * Sets the entity identifier for the property definition.
		 *
		 * @param id entity identifier for the property definition.
		 * @return property definition builder
		 */
		@NonNull
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Sets the internal {@link EntityId} of the artifact that owns this property.
		 *
		 * @param artifact internal entity identifier of the artifact that owns the property.
		 * @return property definition builder
		 */
		@NonNull
		public Builder artifact(Long artifact) {
			return artifact(EntityId.from(artifact));
		}

		/**
		 * Sets the external {@link EntityId} of the artifact that owns this property.
		 *
		 * @param artifact external entity identifier of the artifact that owns the property.
		 * @return property definition builder
		 */
		@NonNull
		public Builder artifact(String artifact) {
			return artifact(EntityId.from(artifact));
		}

		/**
		 * Sets the identifier of the artifact that owns this property.
		 *
		 * @param artifact entity identifier of the artifact that owns the property.
		 * @return property definition builder
		 */
		@NonNull
		public Builder artifact(EntityId artifact) {
			this.artifact = artifact;
			return this;
		}

		/**
		 * Sets the unique checksum identifying this specific configuration definition.
		 *
		 * @param checksum unique checksum identifying this specific configuration definition.
		 * @return property definition builder
		 */
		@NonNull
		public Builder checksum(ByteArray checksum) {
			this.checksum = checksum;
			return this;
		}

		/**
		 * Sets the name of the configuration property.
		 *
		 * @param name name of the configuration property, for example {@code spring.datasource.url}.
		 * @return property definition builder
		 */
		@NonNull
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Sets the property data type.
		 *
		 * @param dataType the property data type.
		 * @return property definition builder
		 */
		@NonNull
		public Builder dataType(String dataType) {
			return dataType(DataType.valueOf(dataType));
		}

		/**
		 * Sets the property data type.
		 *
		 * @param dataType the property data type.
		 * @return property definition builder
		 */
		@NonNull
		public Builder dataType(DataType dataType) {
			this.dataType = dataType;
			return this;
		}

		/**
		 * Sets the logical property type identifier.
		 *
		 * @param type the logical property type identifier.
		 * @return property definition builder
		 */
		@NonNull
		public Builder type(String type) {
			return type(PropertyType.valueOf(type));
		}

		/**
		 * Sets the logical property type identifier.
		 *
		 * @param type the logical property type identifier.
		 * @return property definition builder
		 */
		@NonNull
		public Builder type(PropertyType type) {
			this.type = type;
			return this;
		}

		/**
		 * Sets the fully qualified Java type name.
		 *
		 * @param typeName fully qualified Java type name, for example {@code java.lang.String}.
		 * @return property definition builder
		 */
		@NonNull
		public Builder typeName(String typeName) {
			this.typeName = typeName;
			return this;
		}

		/**
		 * Sets the default value of the property.
		 *
		 * @param defaultValue the default property value.
		 * @return property definition builder
		 */
		@NonNull
		public Builder defaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}

		/**
		 * Sets the human-readable description of the property's purpose.
		 *
		 * @param description human-readable description of the property purpose.
		 * @return property definition builder
		 */
		@NonNull
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Add the value hint describing allowed or example property values.
		 *
		 * @param hint value hint, describing allowed or example property values.
		 * @return property definition builder
		 */
		@NonNull
		public Builder hint(String hint) {
			if (StringUtils.hasText(hint)) {
				this.hints.add(hint);
			}
			return this;
		}

		/**
		 * Sets the list of value hints describing allowed or example property values.
		 *
		 * @param hints value hints, describing allowed or example property values.
		 * @return property definition builder
		 */
		@NonNull
		public Builder hints(Collection<String> hints) {
			if (hints != null) {
				hints.forEach(this::hint);
			}
			return this;
		}

		/**
		 * Sets the deprecation metadata for this property.
		 *
		 * @param deprecation deprecation metadata.
		 * @return property definition builder
		 */
		@NonNull
		public Builder deprecation(Deprecation deprecation) {
			this.deprecation = deprecation;
			return this;
		}

		/**
		 * Sets the number of artifact versions that currently reference this property.
		 *
		 * @param occurrences number of artifact versions that currently reference this property, can't be {@literal null}.
		 * @return property definition builder
		 */
		@NonNull
		public Builder occurrences(int occurrences) {
			this.occurrences = occurrences;
			return this;
		}

		/**
		 * Sets the artifact version identifier where this property first appeared.
		 *
		 * @param firstSeen the artifact version identifier where this property first appeared.
		 * @return property definition builder
		 */
		@NonNull
		public Builder firstSeen(String firstSeen) {
			return firstSeen(Version.of(firstSeen));
		}

		/**
		 * Sets the artifact version identifier where this property first appeared.
		 *
		 * @param firstSeen the artifact version identifier where this property first appeared.
		 * @return property definition builder
		 */
		@NonNull
		public Builder firstSeen(Version firstSeen) {
			this.firstSeen = firstSeen;
			return this;
		}

		/**
		 * Sets the artifact version identifier where this property was last observed.
		 *
		 * @param lastSeen the artifact version identifier where this property was last observed.
		 * @return property definition builder
		 */
		@NonNull
		public Builder lastSeen(String lastSeen) {
			return lastSeen(Version.of(lastSeen));
		}

		/**
		 * Sets the artifact version identifier where this property was last observed.
		 *
		 * @param lastSeen the artifact version identifier where this property was last observed.
		 * @return property definition builder
		 */
		@NonNull
		public Builder lastSeen(Version lastSeen) {
			this.lastSeen = lastSeen;
			return this;
		}

		/**
		 * Builds a new immutable {@link PropertyDefinition} instance.
		 *
		 * @return the constructed property definition, never {@literal null}.
		 * @throws IllegalStateException if any required (non-null) field is missing.
		 */
		@NonNull
		public PropertyDefinition build() {
			Assert.notNull(id, "Property definition entity identifier must not be null");
			Assert.notNull(artifact, "Artifact entity identifier must not be null");
			Assert.notNull(checksum, "Property checksum must not be null");
			Assert.notNull(name, "Property name must not be null");
			Assert.notNull(dataType, "Property data type must not be null");
			Assert.notNull(type, "Property type must not be null");
			Assert.notNull(typeName, "Property type name must not be null");
			Assert.notNull(firstSeen, "First seen version must not be null");
			Assert.notNull(lastSeen, "Last seen version must not be null");

			return new PropertyDefinition(id, artifact, checksum, name, dataType, type, typeName, defaultValue,
					description, List.copyOf(hints), deprecation, occurrences, firstSeen, lastSeen);
		}
	}

}
