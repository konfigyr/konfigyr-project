package com.konfigyr.namespace;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.slug.Slug;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

class NamespaceTest {

	@Test
	@DisplayName("should create namespace using fluent builder")
	void shouldCreateNamespace() {
		final var namespace = Namespace.builder()
				.id(836571L)
				.type("TEAM")
				.slug("test-namespace")
				.name("Test namespace")
				.description("My testing team namespace")
				.createdAt(Instant.now().minus(62, ChronoUnit.DAYS))
				.updatedAt(Instant.now().minus(16, ChronoUnit.HOURS))
				.build();

		assertThat(namespace)
				.returns(EntityId.from(836571L), Namespace::id)
				.returns(NamespaceType.TEAM, Namespace::type)
				.returns("test-namespace", Namespace::slug)
				.returns("Test namespace", Namespace::name)
				.returns("My testing team namespace", Namespace::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isEqualToIgnoringHours(OffsetDateTime.now(ZoneOffset.UTC).minusDays(62))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isEqualToIgnoringMinutes(OffsetDateTime.now(ZoneOffset.UTC).minusHours(16))
				);
	}

	@Test
	@DisplayName("should validate namespace data when using fluent builder")
	void shouldValidateNamespaceBuilder() {
		final var builder = Namespace.builder();

		assertThatThrownBy(builder::build)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace entity identifier can not be null");

		assertThatThrownBy(() -> builder.id("000000BKTH3TG").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace type can not be null");

		assertThatThrownBy(() -> builder.type(NamespaceType.PERSONAL).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace slug can not be blank");

		assertThatThrownBy(() -> builder.slug("test-namespace").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace name can not be blank");

		assertThat(builder.name("Test namespace").build())
				.returns(EntityId.from(12476518224L), Namespace::id)
				.returns(NamespaceType.PERSONAL, Namespace::type)
				.returns("test-namespace", Namespace::slug)
				.returns("Test namespace", Namespace::name)
				.returns(null, Namespace::description)
				.returns(null, Namespace::createdAt)
				.returns(null, Namespace::updatedAt);
	}

	@Test
	@DisplayName("should create namespace definition using fluent builder")
	void shouldCreateNamespaceDefinition() {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.slug("Atreides")
				.name("Atreides")
				.type(NamespaceType.TEAM)
				.description("Atreides Imperium")
				.build();

		assertThat(definition)
				.returns(EntityId.from(1), NamespaceDefinition::owner)
				.returns(Slug.slugify("atreides"), NamespaceDefinition::slug)
				.returns(NamespaceType.TEAM, NamespaceDefinition::type)
				.returns("Atreides", NamespaceDefinition::name)
				.returns("Atreides Imperium", NamespaceDefinition::description);
	}

	@Test
	@DisplayName("should fail to create namespace definition without required fields")
	void shouldValidateNamespaceDefinitionBuilder() {
		final var builder = NamespaceDefinition.builder();

		assertThatThrownBy(builder::build)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace owner can not be null");

		assertThatThrownBy(() -> builder.owner(EntityId.from(1).serialize()).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace type can not be null");

		assertThatThrownBy(() -> builder.type("PERSONAL").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace slug can not be null");

		assertThatThrownBy(() -> builder.slug("Muad'Dib").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace name can not be blank");

		assertThat(builder.name("Muad'Dib").build())
				.returns(EntityId.from(1), NamespaceDefinition::owner)
				.returns(Slug.slugify("muaddib"), NamespaceDefinition::slug)
				.returns(NamespaceType.PERSONAL, NamespaceDefinition::type)
				.returns("Muad'Dib", NamespaceDefinition::name)
				.returns(null, NamespaceDefinition::description);
	}

	@Test
	@DisplayName("should create namespace exists exception without cause")
	void shouldCreateNamespaceExistsException() {
		final var definition = Mockito.mock(NamespaceDefinition.class);
		Mockito.doReturn(Slug.slugify("namespace-slug")).when(definition).slug();

		assertThat(new NamespaceExistsException(definition))
				.hasMessage("Could not create namespace as one already exists with the following slug: namespace-slug")
				.hasNoCause()
				.returns(definition, NamespaceExistsException::getDefinition);
	}

	@Test
	@DisplayName("should create namespace exists exception with cause")
	void shouldCreateNamespaceExistsExceptionWithCause() {
		final var cause = new Exception("cause");
		final var definition = Mockito.mock(NamespaceDefinition.class);
		Mockito.doReturn(Slug.slugify("namespace-slug")).when(definition).slug();

		assertThat(new NamespaceExistsException(definition, cause))
				.hasMessage("Could not create namespace as one already exists with the following slug: namespace-slug")
				.hasCause(cause)
				.returns(definition, NamespaceExistsException::getDefinition);
	}

	@Test
	@DisplayName("should create namespace owner exception without cause")
	void shouldCreateNamespaceOwnerException() {
		final var definition = Mockito.mock(NamespaceDefinition.class);
		Mockito.doReturn(EntityId.from(3)).when(definition).owner();

		assertThat(new NamespaceOwnerException(definition))
				.hasMessageContaining("Could not create namespace as owner does not exists with")
				.hasMessageContaining(definition.owner().toString())
				.hasNoCause()
				.returns(definition, NamespaceOwnerException::getDefinition)
				.returns(EntityId.from(3), NamespaceOwnerException::getOwner);
	}

	@Test
	@DisplayName("should create namespace owner exception with cause")
	void shouldCreateNamespaceOwnerExceptionWithCause() {
		final var cause = new Exception("cause");
		final var definition = Mockito.mock(NamespaceDefinition.class);
		Mockito.doReturn(EntityId.from(13)).when(definition).owner();

		assertThat(new NamespaceOwnerException(definition, cause))
				.hasMessageContaining("Could not create namespace as owner does not exists with")
				.hasMessageContaining(definition.owner().toString())
				.hasCause(cause)
				.returns(definition, NamespaceOwnerException::getDefinition)
				.returns(EntityId.from(13), NamespaceOwnerException::getOwner);
	}

	@Test
	@DisplayName("should create namespace created event")
	void createNamespaceCreatedEvent() {
		assertThat(new NamespaceEvent.Created(EntityId.from(3)))
				.isNotNull()
				.isInstanceOf(NamespaceEvent.Created.class)
				.returns(EntityId.from(3), EntityEvent::id);
	}

}