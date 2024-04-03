package com.konfigyr.slug;

import com.github.slugify.Slugify;
import lombok.EqualsAndHashCode;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Type that defines a URL slug that is used as a part of the URL address that serves as a
 * unique identifier of the page or a resource.
 * <p>
 * For example, our {@link com.konfigyr.namespace.Namespace namespaces} are using URL slugs that
 * look like this: <code>konfigyr.com/ns/<strong>konfigyr</strong></code>
 * <p>
 * The {@link Slug} can contain the following:
 * <ul>
 *     <li>letters <code>a-zA-Z</code></li>
 *     <li>digits <code>0-9</code></li>
 *     <li>hyphens <code>-</code></li>
 *     <li>underscores <code>_</code> - we recommend hyphens instead</li>
 * </ul>
 * and they must:
 * <ul>
 *     <li>start with a letter or digit</li>
 *     <li>not contain special or URL reserved characters</li>
 *     <li>not be longer than 255 characters</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@EqualsAndHashCode
public final class Slug implements Supplier<String>, Serializable {

	private static final Slugify slugify = Slugify.builder()
			.underscoreSeparator(false)
			.transliterator(true)
			.locale(Locale.ENGLISH)
			.lowerCase(false)
			.build();

	private final String value;

	private Slug(String value) {
		this.value = value;
	}

	/**
	 * Creates a new {@link Slug} by applying slug transformations to the given value if needed.
	 *
	 * @param value value to create a {@link Slug} from
	 * @return the {@link Slug}
	 * @throws IllegalArgumentException when the value is blank or {@literal null} or longer than 255 characters
	 */
	@NonNull
	static Slug slugify(@Nullable String value) {
		Assert.notNull(value, "Value to slugify can not be null");
		Assert.hasText(value, "Value to slugify can not be blank");
		Assert.isTrue(255 > value.length(), "Value to slugify is longer than 255 chars: \"" + value + "\".");

		final String slug = slugify.slugify(value);
		Assert.hasText(slug, "Value to slugify is invalid: \"" + value + "\".");

		return new Slug(slug);
	}

	/**
	 * Checks if the given value is a valid {@link Slug}.
	 *
	 * @param value value to be checked
	 * @return {@literal true} if the value is a valid {@link Slug}
	 */
	static boolean isValid(@Nullable String value) {
		if (!StringUtils.hasText(value)) {
			return false;
		}

		final String slug = slugify.slugify(value);
		return StringUtils.hasText(slug) && value.equals(slug);
	}

	/**
	 * Returns a valid URL slug value as a <code>string</code>.
	 *
	 * @return the slug value, never {@literal null}
	 */
	@NonNull
	@Override
	public String get() {
		return value;
	}

	@Override
	public String toString() {
		return get();
	}
}
