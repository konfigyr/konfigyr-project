package com.konfigyr.support;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.*;

class AvatarTest {

	@Test
	@DisplayName("should generate new avatar")
	void shouldGenerateAvatar() {
		final var avatar = Avatar.generate("konfigyr", "k");

		assertThat(avatar)
				.isNotNull()
				.returns("https://avatar.vercel.sh/konfigyr.svg?text=%20K", Avatar::get)
				.returns(URI.create("https://avatar.vercel.sh/konfigyr.svg?text=%20K"), Avatar::uri);
	}

	@Test
	@DisplayName("should generate new avatar with entity identifier")
	void shouldGenerateAvatarFromIdentifier() {
		final var avatar = Avatar.generate(EntityId.from(12476518224L), "JD");

		assertThat(avatar)
				.isNotNull()
				.returns("https://avatar.vercel.sh/000000BKTH3TG.svg?text=JD", Avatar::get)
				.returns(URI.create("https://avatar.vercel.sh/000000BKTH3TG.svg?text=JD"), Avatar::uri);
	}

	@Test
	void shouldCheckEqualsAndHashCode() {
		assertThat(Avatar.parse("https://avatar.vercel.sh/000000BKTH3TG.svg?text=JD"))
				.isEqualTo(Avatar.parse("https://avatar.vercel.sh/000000BKTH3TG.svg?text=JD"))
				.isNotEqualTo(Avatar.parse("https://avatar.vercel.sh/konfigyr.svg?text=K"))
				.hasSameHashCodeAs(Avatar.parse("https://avatar.vercel.sh/000000BKTH3TG.svg?text=JD"))
				.doesNotHaveSameHashCodeAs(Avatar.parse("https://avatar.vercel.sh/konfigyr.svg?text=K"))
				.hasToString("https://avatar.vercel.sh/000000BKTH3TG.svg?text=JD");
	}

	@Test
	@DisplayName("should fail to parse avatar location")
	void shouldFailToParseAvatar() {
		assertThatThrownBy(() -> Avatar.parse(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Avatar URI cannot be blank");

		assertThatThrownBy(() -> Avatar.parse("  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Avatar URI cannot be blank");

		assertThatThrownBy(() -> Avatar.parse("invalid avatar url"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasCauseInstanceOf(URISyntaxException.class);
	}

	@Test
	@DisplayName("should fail to generate new avatar when identifier or text are blank")
	void shouldFailToGenerateAvatar() {
		assertThatThrownBy(() -> Avatar.generate("", ""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Avatar identifier cannot be blank");

		assertThatThrownBy(() -> Avatar.generate("   ", ""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Avatar identifier cannot be blank");

		assertThatThrownBy(() -> Avatar.generate("konfigyr", ""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Avatar text cannot be blank");

		assertThatThrownBy(() -> Avatar.generate("konfigyr", "  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Avatar text cannot be blank");
	}

}