package com.konfigyr.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.util.function.Supplier;

/**
 * Avatar is a graphical representation of a user, the user's character, or persona. Its value
 * is a URL that points to the online resource where it is hosted.
 * <p>
 * This class can generate random avatars based on the user initials if needed. The generator
 * source code can be seen <a href="https://github.com/vercel/avatar">here</a>. Generated avatar
 * URL value is structured like so <code>https://avatar.vercel.sh/{identifier}.svg?text={text}</code>
 *
 * @param uri the avatar location where the resource is located, can't be null
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public record Avatar(@NonNull URI uri) implements Supplier<String>, Serializable {

	@Serial
	private static final long serialVersionUID = 6760302143112950199L;

	static final UriComponents AVATAR_URI = UriComponentsBuilder.fromUriString(
			"https://avatar.vercel.sh/{identifier}.svg?text={text}"
	).build();

	@NonNull
	@Override
	@JsonValue
	public String get() {
		return uri.toString();
	}

	@NonNull
	@Override
	public String toString() {
		return get();
	}

	/**
	 * Attempts to create an {@link Avatar} instance from the given {@link URI} string.
	 *
	 * @param uri resource location, can't be {@literal null}
	 * @return avatar with the given resource location, never {@literal null}
	 * @throws IllegalArgumentException when the URI string is blank or invalid
	 */
	@JsonCreator
	public static Avatar parse(@NonNull String uri) {
		Assert.hasText(uri, "Avatar URI cannot be blank");

		return new Avatar(URI.create(uri));
	}

	/**
	 * Generates a new {@link Avatar} using the given avatar identifier as an {@link com.konfigyr.entity.EntityId}
	 * and avatar image text.
	 *
	 * @param identifier unique avatar entity identifier, can't be {@literal null}
	 * @param text		 avatar image text, can't be {@literal null}
	 * @return the generated Avatar, never {@literal null}
	 */
	@NonNull
	public static Avatar generate(@NonNull EntityId identifier, @NonNull String text) {
		return generate(identifier.serialize(), text);
	}

	/**
	 * Generates a new {@link Avatar} using the given avatar identifier and avatar image text.
	 *
	 * @param identifier unique avatar identifier, can't be {@literal null}
	 * @param text		 avatar image text, can't be {@literal null}
	 * @return the generated Avatar, never {@literal null}
	 */
	@NonNull
	public static Avatar generate(@NonNull String identifier, @NonNull String text) {
		Assert.hasText(identifier, "Avatar identifier cannot be blank");
		Assert.hasText(text, "Avatar text cannot be blank");

		String normalized = text.toUpperCase();

		// avatars using a text with just one character look odd in our UI,
		// adding a space as a first character fixes the problem
		if (normalized.length() == 1) {
			normalized = " " + normalized;
		}

		return new Avatar(AVATAR_URI.expand(identifier, normalized).toUri());
	}

}
