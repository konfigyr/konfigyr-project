package com.konfigyr.vault;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents a logical group of {@link PropertyChange} operations applied as a single unit.
 * <p>
 * {@link PropertyChanges} encapsulates:
 * <ul>
 *     <li>The {@link Profile} to which the changes should be applied</li>
 *     <li>A human-readable title</li>
 *     <li>An optional detailed description of the changes</li>
 *     <li>A set of atomic property changes</li>
 * </ul>
 * <p>
 * This type is used as the input for {@link Vault} mutation operations such as applying changes directly
 * to a {@link Profile} or submitting changes for approval.
 * <p>
 * The {@code subject} typically maps to a Git commit subject line, while {@code description} may be used as
 * the commit body or change request description.
 * <p>
 * All contained {@link PropertyChange} instances are intended to be applied atomically. If any change
 * fails validation, the entire operation must be rejected.
 *
 * @param profile the profile to which the changes should be applied, must not be {@code null}
 * @param subject a short summary of the change, must not be {@code null}
 * @param description an optional detailed explanation, may be {@code null}
 * @param changes the set of atomic property changes, must not be {@code null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public record PropertyChanges(
		Profile profile,
		String subject,
		@Nullable String description,
		Set<PropertyChange> changes
) implements Iterable<PropertyChange>, Serializable {

	@Override
	public Iterator<PropertyChange> iterator() {
		return changes.iterator();
	}

	/**
	 * Returns the size of the property changes that this instance encapsulates.
	 *
	 * @return the size of the property changes.
	 */
	public int size() {
		return changes.size();
	}

	/**
	 * Creates a new {@link Builder} instance used to create a new {@link PropertyChanges} instance.
	 *
	 * @return the property changes builder.
	 */
	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private @Nullable Profile profile;
		private @Nullable String subject;
		private @Nullable String description;
		private final Set<PropertyChange> changes;

		private Builder() {
			this.changes = new LinkedHashSet<>();
		}

		/**
		 * Defines the {@link Profile} that owns the configuration state for which the property
		 * changes would be applied.
		 *
		 * @param profile the profile that owns the configuration state.
		 * @return the property changes builder.
		 */
		public Builder profile(Profile profile) {
			this.profile = profile;
			return this;
		}

		/**
		 * Provide a quick summary that describes why a change is being made.
		 *
		 * @param subject quick summary of the property changes.
		 * @return the property changes builder.
		 */
		public Builder subject(String subject) {
			this.subject = subject;
			return this;
		}

		/**
		 * Description is used to provide additional information about the property changes that should
		 * be applied to the configuration state.
		 *
		 * @param description the description of the property changes.
		 * @return the property changes builder.
		 */
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Registers a property change that creates a new property with the given name and value.
		 *
		 * @param name the name of the property to create.
		 * @param value the value of the property to create.
		 * @return the property changes builder.
		 */
		public Builder createProperty(String name, String value) {
			return add(PropertyChange.create(name, value));
		}

		/**
		 * Registers a property change that modifies an existing property with the given name and value.
		 *
		 * @param name the name of the property to modify.
		 * @param value the value of the property to modify.
		 * @return the property changes builder.
		 */
		public Builder modifyProperty(String name, String value) {
			return add(PropertyChange.modify(name, value));
		}

		/**
		 * Registers a property change that removes an existing property with the given name.
		 *
		 * @param name the name of the property to remove.
		 * @return the property changes builder.
		 */
		public Builder removeProperty(String name) {
			return add(PropertyChange.remove(name));
		}

		/**
		 * Registers a property change instance to be applied to the configuration state.
		 *
		 * @param change the property change to apply.
		 * @return the property changes builder.
		 */
		public Builder add(PropertyChange change) {
			changes.add(change);
			return this;
		}

		/**
		 * Registers property changes to be applied to the configuration state.
		 *
		 * @param changes the property changes to apply.
		 * @return the property changes builder.
		 */
		public Builder add(Iterable<PropertyChange> changes) {
			for (PropertyChange change : changes) {
				add(change);
			}
			return this;
		}

		/**
		 * Creates a new {@link PropertyChanges} instance using the values specified in the builder.
		 *
		 * @return the property changes instance.
		 * @throws IllegalArgumentException when required fields are missing or invalid.
		 */
		public PropertyChanges build() {
			Assert.notNull(profile, "Profile must not be null");
			Assert.hasText(subject, "Subject must not be empty");
			Assert.notEmpty(changes, "Changes must not be empty");

			return new PropertyChanges(profile, subject, description, Collections.unmodifiableSet(changes));
		}
	}

}
