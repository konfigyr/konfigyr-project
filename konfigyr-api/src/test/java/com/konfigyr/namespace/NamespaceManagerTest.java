
package com.konfigyr.namespace;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.membership.Member;
import com.konfigyr.security.NamespaceApplicationSettings;
import com.konfigyr.security.NamespaceClientType;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.OAuthScopes;
import com.konfigyr.support.Avatar;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.support.Slug;
import com.konfigyr.test.AbstractIntegrationTest;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class NamespaceManagerTest extends AbstractIntegrationTest {

	@Autowired
	NamespaceManager manager;

	@Autowired
	com.konfigyr.membership.Memberships members;

	@Test
	@DisplayName("should search namespaces by search term")
	void shouldSearchNamespacesBySearchTerm() {
		final var query = SearchQuery.builder()
				.term("John")
				.build();

		assertThat(manager.search(query))
				.isNotNull()
				.hasSize(1)
				.extracting(Namespace::id)
				.containsExactlyInAnyOrder(EntityId.from(1));
	}

	@Test
	@DisplayName("should search namespaces by account access")
	void shouldSearchNamespacesByAccountAccess() {
		final var query = SearchQuery.builder()
				.criteria(SearchQuery.ACCOUNT, EntityId.from(1L))
				.build();

		assertThat(manager.search(query))
				.isNotNull()
				.hasSize(2)
				.extracting(Namespace::id)
				.containsExactlyInAnyOrder(
						EntityId.from(1),
						EntityId.from(2)
				);
	}

	@Test
	@DisplayName("should search namespaces by namespace slug")
	void shouldSearchNamespacesByNamespace() {
		final var query = SearchQuery.builder()
				.criteria(SearchQuery.NAMESPACE, "konfigyr")
				.build();

		assertThat(manager.search(query))
				.isNotNull()
				.hasSize(1)
				.extracting(Namespace::id)
				.containsExactlyInAnyOrder(
						EntityId.from(2)
				);
	}

	@Test
	@DisplayName("should search namespaces by account access and search term")
	void shouldSearchNamespacesByAccountAccessAndTerm() {
		final var query = SearchQuery.builder()
				.criteria(SearchQuery.ACCOUNT, EntityId.from(1L))
				.term("konf")
				.build();

		assertThat(manager.search(query))
				.isNotNull()
				.hasSize(1)
				.extracting(Namespace::id)
				.containsExactlyInAnyOrder(
						EntityId.from(2)
				);
	}

	@Test
	@DisplayName("should lookup namespace by entity identifier")
	void shouldLookupNamespaceById() {
		final var id = EntityId.from(1);

		assertThat(manager.findById(id))
				.isPresent()
				.get()
				.returns(id, Namespace::id)
				.returns("john-doe", Namespace::slug)
				.returns("John Doe", Namespace::name)
				.returns("Personal namespace for John Doe", Namespace::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(5), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(1), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should lookup namespace by slug path")
	void shouldLookupNamespaceBySlug() {
		final var slug = "konfigyr";

		assertThat(manager.findBySlug(slug))
				.isPresent()
				.get()
				.returns(EntityId.from(2), Namespace::id)
				.returns(slug, Namespace::slug)
				.returns("Konfigyr", Namespace::name)
				.returns("Konfigyr namespace", Namespace::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(1), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should check if namespaces exists by slug path")
	void shouldCheckIfNamespaceExists() {
		assertThat(manager.exists("john-doe")).isTrue();
		assertThat(manager.exists("konfigyr")).isTrue();
		assertThat(manager.exists("unknown")).isFalse();
	}

	@Test
	@DisplayName("should return empty optional when namespace is not found by entity identifier")
	void shouldFailToLookupNamespaceById() {
		assertThat(manager.findById(EntityId.from(991827464))).isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should create namespace for definition")
	void shouldCreateNamespace(AssertablePublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.slug("arakis")
				.name("Arakis")
				.description("Harsh desert planet located in the Canopus star system")
				.build();

		final Namespace namespace = manager.create(definition);

		assertThat(namespace)
				.returns("arakis", Namespace::slug)
				.returns("Arakis", Namespace::name)
				.returns("Harsh desert planet located in the Canopus star system", Namespace::description)
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(400, ChronoUnit.MILLIS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(400, ChronoUnit.MILLIS))
				);

		events.assertThat()
				.contains(NamespaceEvent.Created.class)
				.matching(NamespaceEvent::id, namespace.id());

		assertThat(members.find(namespace))
				.isNotNull()
				.hasSize(1)
				.first()
				.returns(namespace.id(), Member::namespace)
				.returns(definition.owner(), Member::account)
				.returns(NamespaceRole.ADMIN, Member::role)
				.returns("john.doe@konfigyr.com", Member::email)
				.returns("John Doe", Member::displayName)
				.returns(Avatar.generate(definition.owner(), "JD"), Member::avatar)
				.satisfies(it -> assertThat(it.since())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);
	}

	@Test
	@DisplayName("should fail to create namespace with unknown owner")
	void shouldNotCreateNamespaceWithUnknownOwner(AssertablePublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(EntityId.from(999999).serialize())
				.slug("arakis")
				.name("Arakis")
				.build();

		assertThatThrownBy(() -> manager.create(definition))
				.isInstanceOf(NamespaceOwnerException.class)
				.hasNoCause()
				.asInstanceOf(InstanceOfAssertFactories.type(NamespaceOwnerException.class))
				.returns(definition, NamespaceOwnerException::getDefinition)
				.returns(definition.owner(), NamespaceOwnerException::getOwner);

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to create namespace with existing slug")
	void shouldNotCreateNamespaceWithExistingSlug(AssertablePublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.slug("konfigyr")
				.name("Konfigyr")
				.build();

		assertThatThrownBy(() -> manager.create(definition))
				.isInstanceOf(NamespaceExistsException.class)
				.hasCauseInstanceOf(DuplicateKeyException.class)
				.asInstanceOf(InstanceOfAssertFactories.type(NamespaceExistsException.class))
				.extracting(NamespaceExistsException::getDefinition)
				.isEqualTo(definition);

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to create namespace when name is too long")
	void shouldNotCreateNamespaceWithLongNames(AssertablePublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.slug("name-too-long")
				.name(RandomStringUtils.secure().nextAlphabetic(512))
				.build();

		assertThatThrownBy(() -> manager.create(definition))
				.isInstanceOf(NamespaceException.class)
				.hasCauseInstanceOf(DataAccessException.class);

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should update namespace")
	void shouldUpdateNamespace(AssertablePublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.slug("konfigyr")
				.name("Konfigyr Namespace")
				.description("Updated description")
				.build();

		final Namespace namespace = manager.update("konfigyr", definition);

		assertThat(namespace)
				.returns(EntityId.from(2), Namespace::id)
				.returns("konfigyr", Namespace::slug)
				.returns("Konfigyr Namespace", Namespace::name)
				.returns("Updated description", Namespace::description)
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(400, ChronoUnit.MILLIS))
				);

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should rename namespace")
	void shouldRenameNamespace(AssertablePublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.slug("konfigyr-renamed")
				.name("Konfigyr Namespace")
				.build();

		final Namespace namespace = manager.update("konfigyr", definition);

		assertThat(namespace)
				.returns(EntityId.from(2), Namespace::id)
				.returns("konfigyr-renamed", Namespace::slug)
				.returns("Konfigyr Namespace", Namespace::name)
				.returns(null, Namespace::description)
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(400, ChronoUnit.MILLIS))
				);

		events.assertThat()
				.contains(NamespaceEvent.Renamed.class)
				.matching(NamespaceEvent::id, EntityId.from(2))
				.matching(NamespaceEvent.Renamed::from, Slug.slugify("konfigyr"))
				.matching(NamespaceEvent.Renamed::to, Slug.slugify("konfigyr-renamed"));
	}

	@Test
	@DisplayName("should fail to update unknown namespace")
	void shouldNotUpdateUnknownNamespace(AssertablePublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.slug("konfigyr")
				.name("Konfigyr Namespace")
				.build();

		assertThatThrownBy(() -> manager.update("unknown", definition))
				.isInstanceOf(NamespaceNotFoundException.class)
				.hasNoCause();

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to update namespace with an already used slug")
	void shouldNotUpdateNamespaceWithExistingSlug(AssertablePublishedEvents events) {
		final var definition = NamespaceDefinition.builder()
				.owner(1L)
				.slug("konfigyr")
				.name("Konfigyr Namespace")
				.build();

		assertThatThrownBy(() -> manager.update("john-doe", definition))
				.isInstanceOf(NamespaceExistsException.class)
				.hasCauseInstanceOf(DuplicateKeyException.class)
				.asInstanceOf(InstanceOfAssertFactories.type(NamespaceExistsException.class))
				.extracting(NamespaceExistsException::getDefinition)
				.isEqualTo(definition);

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should delete namespace")
	void shouldDeleteNamespace(AssertablePublishedEvents events) {
		assertThatNoException().isThrownBy(() -> manager.delete("konfigyr"));

		assertThat(manager.findBySlug("konfigyr"))
				.isEmpty();

		events.assertThat()
				.contains(NamespaceEvent.Deleted.class)
				.matching(EntityEvent::id, EntityId.from(2));
	}

	@Test
	@DisplayName("should fail to delete unknown namespace")
	void shouldFailToDeleteUnknownNamespace(AssertablePublishedEvents events) {
		assertThatThrownBy(() -> manager.delete("unknown"))
				.isInstanceOf(NamespaceNotFoundException.class)
				.hasNoCause();

		assertThat(events.ofType(NamespaceEvent.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve all namespace applications")
	void shouldSearchApplications() {
		final var query = SearchQuery.builder()
				.pageable(PageRequest.of(0, 10, Sort.by("name").ascending()))
				.build();

		assertThat(manager.findApplications(query))
				.isNotNull()
				.hasSize(7)
				.extracting(NamespaceApplication::id, NamespaceApplication::name)
				.containsExactly(
						tuple(EntityId.from(7), "Agent application"),
						tuple(EntityId.from(2), "Konfigyr active app"),
						tuple(EntityId.from(5), "Konfigyr agent app"),
						tuple(EntityId.from(1), "Konfigyr expired app"),
						tuple(EntityId.from(6), "Konfigyr workload app"),
						tuple(EntityId.from(3), "Personal app"),
						tuple(EntityId.from(4), "Shop app")
				);
	}

	@Test
	@DisplayName("should search namespace applications by term")
	void shouldSearchApplicationsByTerm() {
		final var query = SearchQuery.builder()
				.term("active")
				.build();

		assertThat(manager.findApplications(query))
				.isNotNull()
				.hasSize(1)
				.extracting(NamespaceApplication::id)
				.containsExactlyInAnyOrder(EntityId.from(2));
	}

	@Test
	@DisplayName("should search namespace applications by namespace and identifier")
	void shouldSearchApplicationsByNamespaceAndIdentifier() {
		final var query = SearchQuery.builder()
				.criteria(SearchQuery.NAMESPACE, "konfigyr")
				.criteria(NamespaceApplication.ID_CRITERIA, EntityId.from(2))
				.build();

		assertThat(manager.findApplications(query))
				.isNotNull()
				.hasSize(1)
				.extracting(NamespaceApplication::id)
				.containsExactlyInAnyOrder(EntityId.from(2));
	}

	@Test
	@DisplayName("should search namespace applications that are no longer active")
	void shouldSearchExpiredApplications() {
		final var query = SearchQuery.builder()
				.criteria(NamespaceApplication.ACTIVE_CRITERIA, false)
				.build();

		assertThat(manager.findApplications(query))
				.isNotNull()
				.hasSize(1)
				.extracting(NamespaceApplication::id)
				.containsExactlyInAnyOrder(EntityId.from(1));
	}

	@Test
	@DisplayName("should search namespace applications that are currently active")
	void shouldSearchActiveApplications() {
		final var query = SearchQuery.builder()
				.criteria(NamespaceApplication.ACTIVE_CRITERIA, true)
				.build();

		assertThat(manager.findApplications(query))
				.isNotNull()
				.hasSize(6)
				.extracting(NamespaceApplication::id)
				.containsExactlyInAnyOrder(
						EntityId.from(2),
						EntityId.from(3),
						EntityId.from(4),
						EntityId.from(5),
						EntityId.from(6),
						EntityId.from(7)
				);
	}

	@Test
	@DisplayName("should search service account namespace applications that are currently active")
	void shouldSearchActiveApplicationsByType() {
		final var query = SearchQuery.builder()
				.criteria(NamespaceApplication.ACTIVE_CRITERIA, true)
				.criteria(NamespaceApplication.TYPE_CRITERIA, NamespaceClientType.SERVICE_ACCOUNT)
				.build();

		assertThat(manager.findApplications(query))
				.isNotNull()
				.hasSize(3)
				.extracting(NamespaceApplication::id)
				.containsExactlyInAnyOrder(EntityId.from(2), EntityId.from(3), EntityId.from(4));
	}

	@Test
	@DisplayName("should retrieve namespace application")
	void shouldRetrieveApplication() {
		assertThat(manager.getApplication(EntityId.from(1)))
				.isPresent()
				.get(InstanceOfAssertFactories.type(NamespaceApplication.class))
				.returns(EntityId.from(1), NamespaceApplication::id)
				.returns(EntityId.from(2), NamespaceApplication::namespace)
				.returns(NamespaceClientType.SERVICE_ACCOUNT, NamespaceApplication::type)
				.returns("kfg-AQEAAAAAAAAAAgAAAABqJToWfXkWbVML9iZbEPVai4o", NamespaceApplication::clientId)
				.returns(null, NamespaceApplication::clientSecret)
				.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes)
				.satisfies(it -> assertThat(it.expiresAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(30), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(14), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should fail to retrieve unknown namespace application")
	void shouldRetrieveUnknownNamespaceApplication() {
		assertThat(manager.getApplication(EntityId.from(9999)))
				.isNotNull()
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve agent application settings from the database")
	void shouldRetrieveAgentApplicationSettings() {
		assertThat(manager.getApplication(EntityId.from(5)))
				.isPresent()
				.get(InstanceOfAssertFactories.type(NamespaceApplication.class))
				.returns(NamespaceClientType.AGENT, NamespaceApplication::type)
				.extracting(NamespaceApplication::settings, InstanceOfAssertFactories.type(NamespaceApplicationSettings.AgentSettings.class))
				.returns(List.of("http://localhost/callback", "http://localhost:56789/callback"), NamespaceApplicationSettings.AgentSettings::redirectUris);
	}

	@Test
	@DisplayName("should retrieve workload application settings from the database")
	void shouldRetrieveWorkloadApplicationSettings() {
		assertThat(manager.getApplication(EntityId.from(6)))
				.isPresent()
				.get(InstanceOfAssertFactories.type(NamespaceApplication.class))
				.returns(NamespaceClientType.WORKLOAD, NamespaceApplication::type)
				.extracting(NamespaceApplication::settings, InstanceOfAssertFactories.type(NamespaceApplicationSettings.WorkloadSettings.class))
				.returns("https://token.actions.githubusercontent.com", NamespaceApplicationSettings.WorkloadSettings::issuerUri)
				.returns("repo:konfigyr/*:ref:refs/heads/main", NamespaceApplicationSettings.WorkloadSettings::subjectPattern);
	}

	@Test
	@Transactional
	@DisplayName("should create namespace application without expiration")
	void shouldCreateApplicationWithoutExpiration(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("konfigyr");
		final var definition = NamespaceApplicationDefinition.builder()
				.namespace(namespace)
				.type(NamespaceClientType.SERVICE_ACCOUNT)
				.name("Test expiring service account application")
				.scopes(OAuthScopes.of(OAuthScope.NAMESPACES))
				.build();

		final var application = manager.createApplication(namespace, definition);

		assertThat(application.id())
				.isNotNull();

		assertThat(application.clientId())
				.isNotBlank()
				.hasSize(47);

		assertThat(application.clientSecret())
				.isNotBlank()
				.hasSize(43);

		assertThat(application)
				.returns(definition.namespace(), NamespaceApplication::namespace)
				.returns(definition.type(), NamespaceApplication::type)
				.returns(definition.name(), NamespaceApplication::name)
				.returns(definition.scopes(), NamespaceApplication::scopes)
				.returns(null, NamespaceApplication::expiresAt);

		assertThat(application.createdAt())
				.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));

		assertThat(application.updatedAt())
				.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));

		events.assertThat()
				.contains(NamespaceEvent.ApplicationCreated.class)
				.matching(NamespaceEvent::get, namespace)
				.matching(NamespaceEvent.ApplicationCreated::application, application);
	}

	@Test
	@Transactional
	@DisplayName("should create namespace application without client_secret")
	void shouldCreateApplicationWithoutSecret(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("konfigyr");
		final var definition = NamespaceApplicationDefinition.builder()
				.namespace(namespace)
				.type(NamespaceClientType.AGENT)
				.name("Test expiring AI Agent application")
				.scopes(OAuthScopes.of(OAuthScope.NAMESPACES))
				.build();

		final var application = manager.createApplication(namespace, definition);

		assertThat(application.id())
				.isNotNull();

		assertThat(application.clientId())
				.isNotBlank()
				.hasSize(47);

		assertThat(application.clientSecret())
				.isNull();

		assertThat(application)
				.returns(definition.namespace(), NamespaceApplication::namespace)
				.returns(definition.type(), NamespaceApplication::type)
				.returns(definition.name(), NamespaceApplication::name)
				.returns(definition.scopes(), NamespaceApplication::scopes)
				.returns(null, NamespaceApplication::expiresAt);

		assertThat(application.createdAt())
				.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));

		assertThat(application.updatedAt())
				.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));

		events.assertThat()
				.contains(NamespaceEvent.ApplicationCreated.class)
				.matching(NamespaceEvent::get, namespace)
				.matching(NamespaceEvent.ApplicationCreated::application, application);
	}

	@Test
	@Transactional
	@DisplayName("should create namespace application with expiration")
	void shouldCreateApplicationWithExpiration(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("konfigyr");
		final var definition = NamespaceApplicationDefinition.builder()
				.type(NamespaceClientType.SERVICE_ACCOUNT)
				.namespace(namespace)
				.name("Test OAuth application")
				.scopes(OAuthScopes.of(OAuthScope.NAMESPACES))
				.expiration(OffsetDateTime.now().plusDays(1))
				.build();

		final var application = manager.createApplication(namespace, definition);

		assertThat(application.id())
				.isNotNull();

		assertThat(application.clientId())
				.isNotBlank()
				.hasSize(47);

		assertThat(application.clientSecret())
				.isNotBlank()
				.hasSize(43);

		assertThat(application)
				.returns(definition.namespace(), NamespaceApplication::namespace)
				.returns(definition.type(), NamespaceApplication::type)
				.returns(definition.name(), NamespaceApplication::name)
				.returns(definition.scopes(), NamespaceApplication::scopes);

		assertThat(application.expiresAt())
				.isCloseTo(definition.expiration(), within(10, ChronoUnit.SECONDS));

		assertThat(application.createdAt())
				.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));

		assertThat(application.updatedAt())
				.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));

		events.assertThat()
				.contains(NamespaceEvent.ApplicationCreated.class)
				.matching(NamespaceEvent::get, namespace)
				.matching(NamespaceEvent.ApplicationCreated::application, application);
	}

	@Test
	@Transactional
	@DisplayName("should create agent application and persist its settings")
	void shouldCreateAgentApplicationWithSettings(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("konfigyr");
		final var settings = new NamespaceApplicationSettings.AgentSettings(
				List.of("http://localhost:8080/callback", "http://127.0.0.1:8080/callback")
		);
		final var definition = NamespaceApplicationDefinition.builder()
				.namespace(namespace)
				.type(NamespaceClientType.AGENT)
				.name("Test AI Agent application with settings")
				.scopes(OAuthScopes.of(OAuthScope.NAMESPACES))
				.settings(settings)
				.build();

		final var application = manager.createApplication(namespace, definition);

		assertThat(application)
				.returns(null, NamespaceApplication::clientSecret)
				.returns(settings, NamespaceApplication::settings);

		assertThat(manager.getApplication(application.id()))
				.isPresent()
				.get(InstanceOfAssertFactories.type(NamespaceApplication.class))
				.returns(settings, NamespaceApplication::settings);

		events.assertThat()
				.contains(NamespaceEvent.ApplicationCreated.class)
				.matching(NamespaceEvent::get, namespace)
				.matching(NamespaceEvent.ApplicationCreated::application, application);
	}

	@Test
	@Transactional
	@DisplayName("should create workload application and persist its settings")
	void shouldCreateWorkloadApplicationWithSettings(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("konfigyr");
		final var settings = new NamespaceApplicationSettings.WorkloadSettings(
				"https://gitlab.example.com",
				"project_path:konfigyr/api:ref_type:branch:ref:main"
		);
		final var definition = NamespaceApplicationDefinition.builder()
				.namespace(namespace)
				.type(NamespaceClientType.WORKLOAD)
				.name("Test GitLab CI workload application")
				.scopes(OAuthScopes.of(OAuthScope.NAMESPACES))
				.settings(settings)
				.build();

		final var application = manager.createApplication(namespace, definition);

		assertThat(application)
				.returns(null, NamespaceApplication::clientSecret)
				.returns(settings, NamespaceApplication::settings);

		assertThat(manager.getApplication(application.id()))
				.isPresent()
				.get(InstanceOfAssertFactories.type(NamespaceApplication.class))
				.returns(settings, NamespaceApplication::settings);

		events.assertThat()
				.contains(NamespaceEvent.ApplicationCreated.class)
				.matching(NamespaceEvent::get, namespace)
				.matching(NamespaceEvent.ApplicationCreated::application, application);
	}

	@Test
	@Transactional
	@DisplayName("should update namespace application")
	void shouldUpdateApplication(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("konfigyr");
		final var definition = NamespaceApplicationDefinition.builder()
				.type(NamespaceClientType.SERVICE_ACCOUNT)
				.namespace(namespace)
				.name("Updated OAuth application")
				.scopes(OAuthScopes.of(OAuthScope.PROFILES, OAuthScope.INVITE_MEMBERS))
				.expiration(OffsetDateTime.now().plusDays(2))
				.build();

		final var application = manager.updateApplication(namespace, EntityId.from(3), definition);

		assertThat(application)
				.returns(EntityId.from(3), NamespaceApplication::id)
				.returns(EntityId.from(1), NamespaceApplication::namespace)
				.returns(NamespaceClientType.SERVICE_ACCOUNT, NamespaceApplication::type)
				.returns("Updated OAuth application", NamespaceApplication::name)
				.returns("kfg-AQEAAAAAAAAAAQAAAABqJTlV2OXTvVveqnbXJ21wbPw", NamespaceApplication::clientId)
				.returns(null, NamespaceApplication::clientSecret)
				.returns(OAuthScopes.of(OAuthScope.PROFILES, OAuthScope.INVITE_MEMBERS), NamespaceApplication::scopes);

		assertThat(application.createdAt())
				.isCloseTo(OffsetDateTime.now().minusDays(2), within(1, ChronoUnit.MINUTES));

		assertThat(application.updatedAt())
				.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));

		assertThat(application.expiresAt())
				.isCloseTo(definition.expiration(), within(1, ChronoUnit.MINUTES));

		events.assertThat()
				.contains(NamespaceEvent.ApplicationUpdated.class)
				.matching(NamespaceEvent::get, namespace)
				.matching(NamespaceEvent.ApplicationUpdated::application, application);
	}

	@Test
	@Transactional
	@DisplayName("should reset namespace application")
	void shouldResetApplication(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("john-doe");
		final var application = manager.resetApplication(namespace, EntityId.from(3));

		assertThat(application)
				.returns(EntityId.from(3), NamespaceApplication::id)
				.returns(EntityId.from(1), NamespaceApplication::namespace)
				.returns(NamespaceClientType.SERVICE_ACCOUNT, NamespaceApplication::type)
				.returns("Personal app", NamespaceApplication::name)
				.returns("kfg-AQEAAAAAAAAAAQAAAABqJTlV2OXTvVveqnbXJ21wbPw", NamespaceApplication::clientId)
				.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes)
				.returns(null, NamespaceApplication::expiresAt);

		assertThat(application.clientSecret())
				.isNotBlank()
				.hasSize(43);

		assertThat(application.createdAt())
				.isCloseTo(OffsetDateTime.now().minusDays(2), within(1, ChronoUnit.MINUTES));

		assertThat(application.updatedAt())
				.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));

		events.assertThat()
				.contains(NamespaceEvent.ApplicationReset.class)
				.matching(NamespaceEvent::get, namespace)
				.matching(NamespaceEvent.ApplicationReset::application, application);
	}

	@Test
	@DisplayName("should fail to reset unknown namespace application")
	void shouldResetUnknownApplication(AssertablePublishedEvents events) {
		assertThatExceptionOfType(NamespaceApplicationNotFoundException.class)
				.isThrownBy(() -> manager.resetApplication(lookupNamespace("john-doe"), EntityId.from(999)));

		assertThat(events.eventOfTypeWasPublished(NamespaceEvent.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to reset application that belongs to different namespace")
	void shouldResetApplicationForDifferentNamespace(AssertablePublishedEvents events) {
		assertThatExceptionOfType(NamespaceApplicationNotFoundException.class)
				.isThrownBy(() -> manager.resetApplication(lookupNamespace("konfigyr"), EntityId.from(3)));

		assertThat(events.eventOfTypeWasPublished(NamespaceEvent.class))
				.isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should delete namespace application")
	void shouldRemoveApplication(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("konfigyr");
		assertThatNoException().isThrownBy(() -> manager.removeApplication(namespace, EntityId.from(1)));

		assertThat(manager.getApplication(EntityId.from(1)))
				.isEmpty();

		events.assertThat()
				.contains(NamespaceEvent.ApplicationRemoved.class)
				.matching(NamespaceEvent::get, namespace)
				.matching(event -> event.application().id(), EntityId.from(1));
	}

	@Test
	@DisplayName("should delete an unknown namespace application")
	void shouldRemoveUnknownApplication(AssertablePublishedEvents events) {
		assertThatExceptionOfType(NamespaceApplicationNotFoundException.class)
				.isThrownBy(() -> manager.removeApplication(lookupNamespace("john-doe"), EntityId.from(999)));

		assertThat(events.eventOfTypeWasPublished(NamespaceEvent.class))
				.isFalse();
	}

	@Test
	@DisplayName("should retrieve all trusted issuers for namespace")
	void shouldFindTrustedIssuers() {
		final var namespace = lookupNamespace("konfigyr");
		final var query = SearchQuery.builder()
				.pageable(PageRequest.of(0, 10, Sort.by("name").ascending()))
				.build();

		assertThat(manager.findTrustedIssuers(namespace, query))
				.isNotNull()
				.hasSize(3)
				.extracting(NamespaceTrustedIssuer::id, NamespaceTrustedIssuer::name)
				.containsExactly(
						tuple(EntityId.from(3), "Disabled issuer"),
						tuple(EntityId.from(1), "Konfigyr CI"),
						tuple(EntityId.from(2), "Konfigyr staging CI")
				);
	}

	@Test
	@DisplayName("should retrieve only active trusted issuers for namespace")
	void shouldFindActiveTrustedIssuers() {
		final var namespace = lookupNamespace("konfigyr");
		final var query = SearchQuery.builder()
				.criteria(NamespaceTrustedIssuer.ACTIVE_CRITERIA, true)
				.build();

		assertThat(manager.findTrustedIssuers(namespace, query))
				.isNotNull()
				.hasSize(2)
				.extracting(NamespaceTrustedIssuer::id)
				.containsExactlyInAnyOrder(EntityId.from(1), EntityId.from(2));
	}

	@Test
	@DisplayName("should retrieve trusted issuers for namespace matching the search term")
	void shouldRetrieveMatchingTrustedIssuers() {
		final var namespace = lookupNamespace("john-doe");
		final var query = SearchQuery.builder()
				.term("john-doe.example")
				.build();

		assertThat(manager.findTrustedIssuers(namespace, query))
				.isNotNull()
				.hasSize(1)
				.extracting(NamespaceTrustedIssuer::id)
				.containsExactlyInAnyOrder(EntityId.from(4));
	}

	@Test
	@DisplayName("should retrieve trusted issuers belonging to the requested namespace only")
	void shouldFindTrustedIssuersForNamespaceOnly() {
		final var namespace = lookupNamespace("john-doe");
		final var query = SearchQuery.builder().build();

		assertThat(manager.findTrustedIssuers(namespace, query))
				.isNotNull()
				.hasSize(1)
				.extracting(NamespaceTrustedIssuer::id)
				.containsExactly(EntityId.from(4));
	}

	@Test
	@DisplayName("should retrieve trusted issuer by entity identifier")
	void shouldGetTrustedIssuer() {
		final var namespace = lookupNamespace("konfigyr");

		assertThat(manager.getTrustedIssuer(namespace, EntityId.from(1)))
				.isPresent()
				.get()
				.returns(EntityId.from(1), NamespaceTrustedIssuer::id)
				.returns(EntityId.from(2), NamespaceTrustedIssuer::namespace)
				.returns("Konfigyr CI", NamespaceTrustedIssuer::name)
				.returns("GitHub Actions for Konfigyr org", NamespaceTrustedIssuer::description)
				.returns("https://ci.konfigyr.com", NamespaceTrustedIssuer::issuerUri)
				.returns("https://ci.konfigyr.com/jwks.json", NamespaceTrustedIssuer::jwksUri)
				.returns(true, NamespaceTrustedIssuer::active)
				.satisfies(it -> assertThat(it.allowedAudiences()).containsExactly("konfigyr-api"))
				.satisfies(it -> assertThat(it.customClaims()).containsEntry("environment", "production"));
	}

	@Test
	@DisplayName("should return empty when trusted issuer belongs to a different namespace")
	void shouldNotGetTrustedIssuerFromDifferentNamespace() {
		final var namespace = lookupNamespace("konfigyr");

		assertThat(manager.getTrustedIssuer(namespace, EntityId.from(4)))
				.isEmpty();
	}

	@Test
	@DisplayName("should return empty when trusted issuer does not exist")
	void shouldNotGetUnknownTrustedIssuer() {
		final var namespace = lookupNamespace("konfigyr");

		assertThat(manager.getTrustedIssuer(namespace, EntityId.from(9999)))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should create trusted issuer for namespace")
	void shouldCreateTrustedIssuer(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("konfigyr");
		final var definition = NamespaceTrustedIssuerDefinition.builder()
				.name("New Jenkins CI")
				.description("Self-hosted Jenkins instance")
				.issuerUri("https://jenkins.konfigyr.com")
				.allowedAudience("konfigyr-api")
				.customClaim("environment", "production")
				.build();

		final var issuer = manager.createTrustedIssuer(namespace, definition);

		assertThat(issuer.id()).isNotNull();
		assertThat(issuer)
				.returns(namespace.id(), NamespaceTrustedIssuer::namespace)
				.returns("New Jenkins CI", NamespaceTrustedIssuer::name)
				.returns("Self-hosted Jenkins instance", NamespaceTrustedIssuer::description)
				.returns("https://jenkins.konfigyr.com", NamespaceTrustedIssuer::issuerUri)
				.returns(null, NamespaceTrustedIssuer::jwksUri)
				.returns(true, NamespaceTrustedIssuer::active)
				.satisfies(it -> assertThat(it.allowedAudiences()).containsExactly("konfigyr-api"))
				.satisfies(it -> assertThat(it.customClaims()).containsEntry("environment", "production"));

		assertThat(issuer.createdAt()).isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));
		assertThat(issuer.updatedAt()).isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));

		assertThat(manager.getTrustedIssuer(namespace, issuer.id())).isPresent();

		events.assertThat()
				.contains(NamespaceEvent.TrustedIssuerCreated.class)
				.matching(NamespaceEvent::get, namespace)
				.matching(NamespaceEvent.TrustedIssuerCreated::issuer, issuer);
	}

	@Test
	@Transactional
	@DisplayName("should update trusted issuer")
	void shouldUpdateTrustedIssuer(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("konfigyr");
		final var definition = NamespaceTrustedIssuerDefinition.builder()
				.name("Konfigyr CI updated")
				.issuerUri("https://ci.konfigyr.com")
				.jwksUri("https://ci.konfigyr.com/updated-jwks.json")
				.allowedAudience("konfigyr-api")
				.allowedAudience("konfigyr-cli")
				.build();

		final var issuer = manager.updateTrustedIssuer(namespace, EntityId.from(1), definition);

		assertThat(issuer)
				.returns(EntityId.from(1), NamespaceTrustedIssuer::id)
				.returns(namespace.id(), NamespaceTrustedIssuer::namespace)
				.returns("Konfigyr CI updated", NamespaceTrustedIssuer::name)
				.returns(null, NamespaceTrustedIssuer::description)
				.returns("https://ci.konfigyr.com", NamespaceTrustedIssuer::issuerUri)
				.returns("https://ci.konfigyr.com/updated-jwks.json", NamespaceTrustedIssuer::jwksUri)
				.satisfies(it -> assertThat(it.allowedAudiences())
						.containsExactlyInAnyOrder("konfigyr-api", "konfigyr-cli"))
				.satisfies(it -> assertThat(it.customClaims()).isEmpty());

		assertThat(issuer.updatedAt()).isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));

		events.assertThat()
				.contains(NamespaceEvent.TrustedIssuerUpdated.class)
				.matching(NamespaceEvent::get, namespace)
				.matching(NamespaceEvent.TrustedIssuerUpdated::issuer, issuer);
	}

	@Test
	@DisplayName("should fail to update trusted issuer from a different namespace")
	void shouldNotUpdateTrustedIssuerFromDifferentNamespace(AssertablePublishedEvents events) {
		final var definition = NamespaceTrustedIssuerDefinition.builder()
				.name("Attempt")
				.issuerUri("https://ci.konfigyr.com")
				.build();

		assertThatExceptionOfType(NamespaceTrustedIssuerNotFoundException.class)
				.isThrownBy(() -> manager.updateTrustedIssuer(lookupNamespace("john-doe"), EntityId.from(1), definition));

		assertThat(events.eventOfTypeWasPublished(NamespaceEvent.TrustedIssuerUpdated.class)).isFalse();
	}

	@Test
	@DisplayName("should fail to update unknown trusted issuer")
	void shouldNotUpdateUnknownTrustedIssuer(AssertablePublishedEvents events) {
		final var definition = NamespaceTrustedIssuerDefinition.builder()
				.name("Attempt")
				.issuerUri("https://unknown.example.com")
				.build();

		assertThatExceptionOfType(NamespaceTrustedIssuerNotFoundException.class)
				.isThrownBy(() -> manager.updateTrustedIssuer(lookupNamespace("konfigyr"), EntityId.from(9999), definition));

		assertThat(events.eventOfTypeWasPublished(NamespaceEvent.TrustedIssuerUpdated.class)).isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should delete trusted issuer")
	void shouldRemoveTrustedIssuer(AssertablePublishedEvents events) {
		final var namespace = lookupNamespace("konfigyr");
		assertThatNoException().isThrownBy(() -> manager.removeTrustedIssuer(namespace, EntityId.from(1)));

		assertThat(manager.getTrustedIssuer(namespace, EntityId.from(1))).isEmpty();

		events.assertThat()
				.contains(NamespaceEvent.TrustedIssuerRemoved.class)
				.matching(NamespaceEvent::get, namespace)
				.matching(event -> event.issuer().id(), EntityId.from(1));
	}

	@Test
	@DisplayName("should fail to delete trusted issuer from a different namespace")
	void shouldNotRemoveTrustedIssuerFromDifferentNamespace(AssertablePublishedEvents events) {
		assertThatExceptionOfType(NamespaceTrustedIssuerNotFoundException.class)
				.isThrownBy(() -> manager.removeTrustedIssuer(lookupNamespace("john-doe"), EntityId.from(1)));

		assertThat(events.eventOfTypeWasPublished(NamespaceEvent.TrustedIssuerRemoved.class)).isFalse();
	}

	@Test
	@DisplayName("should fail to delete unknown trusted issuer")
	void shouldNotRemoveUnknownTrustedIssuer(AssertablePublishedEvents events) {
		assertThatExceptionOfType(NamespaceTrustedIssuerNotFoundException.class)
				.isThrownBy(() -> manager.removeTrustedIssuer(lookupNamespace("konfigyr"), EntityId.from(9999)));

		assertThat(events.eventOfTypeWasPublished(NamespaceEvent.TrustedIssuerRemoved.class)).isFalse();
	}

	private Namespace lookupNamespace(String slug) {
		return assertThat(manager.findBySlug(slug))
				.as("Namespace with slug '%s' not found", slug)
				.isPresent()
				.get()
				.actual();
	}

}
