package com.konfigyr.security.provision;

import com.konfigyr.namespace.NamespaceType;
import com.konfigyr.support.FullName;
import com.konfigyr.support.Slug;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vladimir Spasic
 **/
class ProvisioningHintsTest {

	@Test
	@DisplayName("should create hints from github")
	void shouldCreateHintsFromGithub() {
		final var user = createUser(Map.of(
				"login", "john.doe",
				"email", "john.doe@konfigyr.com",
				"name", "John Doe",
				"avatar_url", "https://example.com/avatar.gif"
		));

		assertThat(ProvisioningHints.from(user, "github"))
				.isNotNull()
				.returns("john.doe@konfigyr.com", ProvisioningHints::getEmail)
				.returns(FullName.of("John", "Doe"), ProvisioningHints::getName)
				.returns(URI.create("https://example.com/avatar.gif"), ProvisioningHints::getAvatar)
				.returns(Slug.slugify("john.doe"), ProvisioningHints::getNamespace)
				.returns(NamespaceType.PERSONAL, ProvisioningHints::getType);
	}

	@Test
	@DisplayName("should create hints from gitlab")
	void shouldCreateHintsFromGitlab() {
		final var user = createUser(Map.of(
				"username", "john.doe",
				"email", "john.doe@konfigyr.com",
				"name", "John Doe",
				"avatar_url", "https://example.com/avatar.gif"
		));

		assertThat(ProvisioningHints.from(user, "gitlab"))
				.isNotNull()
				.returns("john.doe@konfigyr.com", ProvisioningHints::getEmail)
				.returns(FullName.of("John", "Doe"), ProvisioningHints::getName)
				.returns(URI.create("https://example.com/avatar.gif"), ProvisioningHints::getAvatar)
				.returns(Slug.slugify("john.doe"), ProvisioningHints::getNamespace)
				.returns(NamespaceType.PERSONAL, ProvisioningHints::getType);
	}

	@Test
	@DisplayName("should create hints from unsupported provider")
	void shouldCreateOpenIdHints() {
		final var user = createUser(Map.of(
				"preferred_username", "john.doe",
				"email", "john.doe@konfigyr.com",
				"picture", "https://example.com/avatar.gif"
		));

		assertThat(ProvisioningHints.from(user, "unknown"))
				.isNotNull()
				.returns("john.doe@konfigyr.com", ProvisioningHints::getEmail)
				.returns(null, ProvisioningHints::getName)
				.returns(URI.create("https://example.com/avatar.gif"), ProvisioningHints::getAvatar)
				.returns(Slug.slugify("john.doe"), ProvisioningHints::getNamespace)
				.returns(NamespaceType.PERSONAL, ProvisioningHints::getType);
	}

	@Test
	@DisplayName("should create hints from provisioning required exception constructor")
	void shouldCreateHintsFromException() {
		final var user = createUser(Map.of(
				"preferred_username", "",
				"email", "john.doe@konfigyr.com",
				"picture", "invalid avatar picture"
		));

		assertThat(new ProvisioningRequiredException())
				.extracting(ProvisioningRequiredException::getHints)
				.isEqualTo(ProvisioningHints.EMPTY);

		assertThat(new ProvisioningRequiredException(user, "unknown"))
				.extracting(ProvisioningRequiredException::getHints)
				.returns("john.doe@konfigyr.com", ProvisioningHints::getEmail)
				.returns(null, ProvisioningHints::getName)
				.returns(null, ProvisioningHints::getAvatar)
				.returns(null, ProvisioningHints::getNamespace)
				.returns(NamespaceType.PERSONAL, ProvisioningHints::getType);
	}

	static OAuth2User createUser(Map<String, Object> attributes) {
		return new DefaultOAuth2User(
				AuthorityUtils.createAuthorityList("test-scope"),
				attributes,
				"email"
		);
	}

}
