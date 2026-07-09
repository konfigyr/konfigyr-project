package com.konfigyr.audit;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditMessageResolverTest {

	private AuditMessageResolver resolver;

	@BeforeEach
	void setup() {
		final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename("messages/audit");
		messageSource.setDefaultEncoding("UTF-8");

		resolver = new AuditMessageResolver(messageSource);
	}

	@Test
	@DisplayName("should resolve message for event without details")
	void shouldResolveMessageWithoutDetails() {
		assertThat(resolver.resolve("namespace.created", null))
				.isEqualTo("Namespace was created");
	}

	@Test
	@DisplayName("should resolve message with detail arguments")
	void shouldResolveMessageWithDetails() {
		final Map<String, Object> details = Map.of("from", "old-slug", "to", "new-slug");

		assertThat(resolver.resolve("namespace.renamed", details))
				.isEqualTo("Namespace was renamed from 'old-slug' to 'new-slug'");
	}

	@Test
	@DisplayName("should resolve message for profile event with name parameter")
	void shouldResolveProfileMessage() {
		assertThat(resolver.resolve("profile.created", Map.of("name", "production")))
				.isEqualTo("Profile 'production' was created");
	}

	@Test
	@DisplayName("should resolve message for keyset event")
	void shouldResolveKeysetMessage() {
		assertThat(resolver.resolve("keyset.rotated", null))
				.isEqualTo("Keyset was rotated");
	}

	@Test
	@DisplayName("should resolve message for artifact release event")
	void shouldResolveArtifactMessage() {
		assertThat(resolver.resolve("artifact-version.publication-completed",
				Map.of("coordinates", "com.konfigyr:api:1.0.0")))
				.isEqualTo("Artifact com.konfigyr:api:1.0.0 has been successfully published");
	}

	@Test
	@DisplayName("should resolve message for member added event with role")
	void shouldResolveMemberAddedMessage() {
		final Map<String, Object> details = Map.of("account", EntityId.from(42), "role", "ADMIN");

		assertThat(resolver.resolve("namespace.member-added", details))
				.isEqualTo("Member was added with ADMIN role");
	}

	@Test
	@DisplayName("should fall back to event type for unknown events")
	void shouldFallBackToEventType() {
		assertThat(resolver.resolve("custom.something", null))
				.isEqualTo("custom.something");
	}

}
