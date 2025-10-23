package com.konfigyr.artifactory;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

/**
 * Interface that provides the description, or metadata, of a single Spring Boot configuration property.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public interface PropertyMetadata extends Comparable<PropertyMetadata>, Serializable {

	/**
	 * The full name of the configuration property, for example {@code spring.datasource.url}.
	 *
	 * @return the property name, can't be {@literal null}.
	 **/
	@NonNull String name();

	/**
	 * The property logical type identifier that is derived from the original property type name.
	 *
	 * @return the property type, can't be {@literal null}.
	 **/
	@NonNull PropertyType type();

	/**
	 * Data type category used to define the actual data type of the value that can be assigned to
	 * a property defined by this {@link PropertyMetadata}.
	 *
	 * @return the property data type, can't be {@literal null}.
	 **/
	@NonNull DataType dataType();

	/**
	 * The fully qualified Java type name that is present in the Spring Boot configuration metadata source,
	 * for example {@code java.lang.String} or {@code java.time.Duration}.
	 *
	 * @return the property Java type name, can't be {@literal null}.
	 **/
	@NonNull String typeName();

	/**
	 * Default value of the configuration property, if defined.
	 *
	 * @return the default property value, can be {@literal null}.
	 */
	@NonNull String defaultValue();

	/**
	 * Human-readable description of the property purpose, if defined.
	 *
	 * @return the property description, can be {@literal null}.
	 */
	@NonNull String description();

	/**
	 * Sorted collection that contains hints or data describing allowed or example values that can
	 * be assigned to a property defined by this {@link PropertyMetadata}.
	 *
	 * @return list of possible configuration property values, can't be {@literal null}.
	 */
	@NonNull List<String> hints();

	/**
	 * The deprecation information, if available.
	 *
	 * @return the deprecation metadata, may be {@literal null}.
	 */
	@Nullable Deprecation deprecation();

	@Override
	default int compareTo(@Nonnull PropertyMetadata o) {
		return Comparator.comparing(PropertyMetadata::name).compare(this, o);
	}

}
