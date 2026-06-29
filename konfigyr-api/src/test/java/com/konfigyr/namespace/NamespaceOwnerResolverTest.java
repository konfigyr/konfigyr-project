package com.konfigyr.namespace;

import com.konfigyr.artifactory.Owner;
import com.konfigyr.artifactory.OwnerNotFoundException;
import com.konfigyr.artifactory.OwnerResolver;
import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NamespaceOwnerResolverTest {

	final Namespace namespace = Namespace.builder()
			.id(126782L)
			.slug("owning-namespace")
			.name("Owning namespace")
			.build();

	@Mock
	NamespaceManager namespaces;

	OwnerResolver resolver;

	@BeforeEach
	void setup() {
		resolver = new NamespaceOwnerResolver(namespaces);
	}

	@Test
	@DisplayName("should resolve owner for the given namespace slug")
	void shouldResolveOwnerBySlug() {
		doReturn(Optional.of(namespace)).when(namespaces).findBySlug("owning-namespace");

		assertThat(resolver.resolve("owning-namespace"))
				.returns(namespace.id(), Owner::id)
				.returns(namespace.slug(), Owner::slug);
	}

	@Test
	@DisplayName("should resolve owner for the given namespace id")
	void shouldResolveOwnerById() {
		doReturn(Optional.of(namespace)).when(namespaces).findById(EntityId.from(126782L));

		assertThat(resolver.resolve(EntityId.from(126782L)))
				.returns(namespace.id(), Owner::id)
				.returns(namespace.slug(), Owner::slug);
	}

	@Test
	@DisplayName("should throw owner not found for an unknown namespace slug")
	void shouldThrowOwnerNotFoundUnknownSlug() {
		assertThatExceptionOfType(OwnerNotFoundException.class)
				.isThrownBy(() -> resolver.resolve("unknown-namespace"))
				.withMessage("Could not find an owner with the following name: %s", "unknown-namespace");

		verify(namespaces).findBySlug("unknown-namespace");
	}

	@Test
	@DisplayName("should throw owner not found for an unknown namespace id")
	void shouldThrowOwnerNotFoundForUnknownId() {
		assertThatExceptionOfType(OwnerNotFoundException.class)
				.isThrownBy(() -> resolver.resolve(EntityId.from(9999L)))
				.withMessage("Could not find an owner with the following identifier: %s", EntityId.from(9999L));

		verify(namespaces).findById(EntityId.from(9999L));
	}

}
