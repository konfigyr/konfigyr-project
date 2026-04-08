package com.konfigyr.vault.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.CollectionModel;
import com.konfigyr.hateoas.CursorModel;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.Services;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import com.konfigyr.vault.*;
import com.konfigyr.vault.history.RevisionNotFoundException;
import com.konfigyr.vault.state.GitStateRepository;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static com.konfigyr.vault.controller.VaultProfileControllerTest.profileNotFound;

class VaultControllerTest extends AbstractControllerTest {

	@Autowired
	VaultProperties properties;

	@Autowired
	ProfileManager profiles;

	@Autowired
	Services services;

	Service service;
	GitStateRepository repository;

	@BeforeEach
	void setup() {
		service = services.get(EntityId.from(2)).orElseThrow();
		repository = GitStateRepository.initialize(service, properties.getRepositoryDirectory());
	}

	@AfterEach
	void cleanup() {
		repository.destroy();
		repository.close();
	}

	@Test
	@DisplayName("should retrieve an empty configuration state for a service profile")
	void emptyConfigurationStateForProfile() {
		final var profile = prepareServiceProfile("staging");

		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/properties", "konfigyr", service.slug(), profile.slug())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(InstanceOfAssertFactories.map(String.class, String.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve a change history for a service profile")
	void retrieveProfileChangeHistory() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/history", "konfigyr", service.slug(), "locked")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(cursorModel(ChangeHistory.class))
				.satisfies(it -> assertThat(it.getMetadata())
						.isNotNull()
						.returns(7L, CursorModel.CursorMetadata::size)
						.returns(null, CursorModel.CursorMetadata::next)
						.returns(null, CursorModel.CursorMetadata::previous)
				)
				.extracting(CollectionModel::getContent, InstanceOfAssertFactories.iterable(ChangeHistory.class))
				.hasSize(7)
				.extracting(ChangeHistory::id, ChangeHistory::revision, ChangeHistory::subject)
				.containsExactly(
						tuple(EntityId.from(7), "last-revision", "Last change"),
						tuple(EntityId.from(6), "sixth-revision", "Sixth change"),
						tuple(EntityId.from(5), "fifth-revision", "Fifth change"),
						tuple(EntityId.from(4), "fourth-revision", "Fourth change"),
						tuple(EntityId.from(3), "third-revision", "Third change"),
						tuple(EntityId.from(2), "second-revision", "Second change"),
						tuple(EntityId.from(1), "first-revision", "First change")
				);
	}

	@Test
	@DisplayName("should fail to retrieve profile configuration state from an unknown profile")
	void retrieveStateForUnknownProfile() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/properties", "konfigyr", service.slug(), "unknown")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(profileNotFound("unknown"));
	}

	@Test
	@DisplayName("should fail to retrieve profile configuration state from an unknown service")
	void retrieveStateForUnknownService() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/properties", "konfigyr", "unknown-service", "live")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should fail to retrieve profile configuration state from an unknown namespace")
	void retrieveStateForUnknownNamespace() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/properties", "unknown-namespace", service.slug(), "staging")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to retrieve profile configuration state when user is not a member of a namespace")
	void retrieveStateWithoutMembership() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/properties", "john-doe", "john-doe-blog", "live")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to retrieve profile configuration state without required scope")
	void retrieveStateWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/properties", "john-doe", "john-doe-blog", "live")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_PROFILES));
	}

	@Test
	@DisplayName("should apply property changes to the configuration state for a service profile")
	void applyChangesForProfile() {
		final var profile = prepareServiceProfile("development");

		final var result = mvc.post().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/apply", "konfigyr", service.slug(), profile.slug())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Test changes\",\"changes\":[{\"name\":\"server.port\",\"value\":\"8080\",\"operation\":\"CREATE\"}]}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(VaultController.RevisionInformation.class)
				.returns("Test changes", VaultController.RevisionInformation::subject)
				.returns(null, VaultController.RevisionInformation::description)
				.returns("John Doe", VaultController.RevisionInformation::author)
				.satisfies(it -> assertThat(it.revision())
						.isNotBlank()
						.matches("[a-f0-9]{40}")
				)
				.satisfies(it -> assertThat(it.timestamp())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				).actual();

		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/properties", "konfigyr", service.slug(), profile.slug())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(InstanceOfAssertFactories.map(String.class, String.class))
				.hasSize(1)
				.containsEntry("server.port", "8080");

		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/history/{revision}", "konfigyr", service.slug(), profile.slug(), result.revision())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(cursorModel(ChangeHistoryRecord.class))
				.extracting(CursorModel::getContent, InstanceOfAssertFactories.iterable(ChangeHistoryRecord.class))
				.hasSize(1)
				.first()
				.returns(result.revision(), ChangeHistoryRecord::revision)
				.returns("server.port", ChangeHistoryRecord::name)
				.returns(PropertyTransitionType.ADDED, ChangeHistoryRecord::action)
				.returns(null, ChangeHistoryRecord::from)
				.returns("8080", ChangeHistoryRecord::to)
				.returns(result.author(), ChangeHistoryRecord::appliedBy)
				.returns(result.timestamp(), ChangeHistoryRecord::appliedAt)
				.satisfies(it -> assertThat(it.id())
						.hasSize(32)
						.isHexadecimal()
				);

		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/property/{property}/history", "konfigyr", service.slug(), profile.slug(), "server.port")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(collectionModel(ChangeHistoryRecord.class))
				.extracting(CollectionModel::getContent, InstanceOfAssertFactories.iterable(ChangeHistoryRecord.class))
				.hasSize(1)
				.first()
				.returns(result.revision(), ChangeHistoryRecord::revision)
				.returns("server.port", ChangeHistoryRecord::name)
				.returns(PropertyTransitionType.ADDED, ChangeHistoryRecord::action)
				.returns(null, ChangeHistoryRecord::from)
				.returns("8080", ChangeHistoryRecord::to)
				.returns(result.author(), ChangeHistoryRecord::appliedBy)
				.returns(result.timestamp(), ChangeHistoryRecord::appliedAt)
				.satisfies(it -> assertThat(it.id())
						.hasSize(32)
						.isHexadecimal()
				);
	}

	@Test
	@DisplayName("should fail to apply property changes to protected service profile")
	void applyChangesToProtectedProfile() {
		final var profile = prepareServiceProfile("staging");

		mvc.post().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/apply", "konfigyr", service.slug(), profile.slug())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Test changes\",\"changes\":[{\"name\":\"server.port\",\"value\":\"8080\",\"operation\":\"CREATE\"}]}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.CONFLICT, problem -> problem
						.hasTitle("Profile is protected")
						.hasDetailContaining("The %s profile is protected. Changes must be submitted for approval instead of being applied directly", profile.name())
				).andThen(hasFailedWithException(ProfilePolicyViolationException.class, ex -> ex
						.hasMessageContaining("Profile '%s' is protected. Changes must be submitted for approval.", profile.slug())
						.returns(ProfilePolicyViolationException.ViolationReason.PROTECTED_PROFILE, ProfilePolicyViolationException::getReason)
				)));
	}

	@Test
	@DisplayName("should fail to apply property changes to read-only service profile")
	void applyChangesToReadOnlyProfile() {
		final var profile = prepareServiceProfile("locked");

		mvc.post().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/apply", "konfigyr", service.slug(), profile.slug())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Test changes\",\"changes\":[{\"name\":\"server.port\",\"value\":\"8080\",\"operation\":\"CREATE\"}]}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.CONFLICT, problem -> problem
						.hasTitle("Profile is read-only")
						.hasDetailContaining("The %s profile is in a read-only state. Profiles in this state do not allow state modifications.", profile.name())
				).andThen(hasFailedWithException(ProfilePolicyViolationException.class, ex -> ex
						.hasMessageContaining("Profile '%s' is read-only and does not allow state modifications.", profile.slug())
						.returns(ProfilePolicyViolationException.ViolationReason.IMMUTABLE_PROFILE, ProfilePolicyViolationException::getReason)
				)));
	}

	@Test
	@DisplayName("should fail to apply property changes due to invalid change request payload")
	void applyChangesWithInvalidPayload() {
		final var profile = prepareServiceProfile("staging");

		mvc.post().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/apply", "konfigyr", service.slug(), profile.slug())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitleContaining("Invalid")
						.hasDetailContaining("invalid request data")
						.hasPropertySatisfying("errors", errors -> assertThat(errors)
								.isNotNull()
								.isInstanceOf(Collection.class)
								.asInstanceOf(InstanceOfAssertFactories.collection(Map.class))
								.extracting("pointer")
								.containsExactlyInAnyOrder("changes", "name")
						)
				));
	}

	@Test
	@DisplayName("should fail to apply property changes to an unknown profile")
	void applyChangesToUnknownProfile() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/apply", "konfigyr", service.slug(), "unknown")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Test changes\",\"changes\":[{\"name\":\"server.port\",\"value\":\"8080\",\"operation\":\"CREATE\"}]}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(profileNotFound("unknown"));
	}

	@Test
	@DisplayName("should fail to apply property changes to an unknown service")
	void applyChangesToUnknownService() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/apply", "konfigyr", "unknown-service", "live")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Test changes\",\"changes\":[{\"name\":\"server.port\",\"value\":\"8080\",\"operation\":\"CREATE\"}]}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should fail to apply property changes to an unknown namespace")
	void applyChangesToUnknownNamespace() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/apply", "unknown-namespace", service.slug(), "staging")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Test changes\",\"changes\":[{\"name\":\"server.port\",\"value\":\"8080\",\"operation\":\"CREATE\"}]}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to apply property changes when user is not a member of a namespace")
	void applyChangesWithoutMembership() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/apply", "john-doe", "john-doe-blog", "live")
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Test changes\",\"changes\":[{\"name\":\"server.port\",\"value\":\"8080\",\"operation\":\"CREATE\"}]}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to apply property changes without required scope")
	void applyChangesWithoutScope() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/apply", "john-doe", "john-doe-blog", "live")
				.with(authentication(TestPrincipals.john()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Test changes\",\"changes\":[{\"name\":\"server.port\",\"value\":\"8080\",\"operation\":\"CREATE\"}]}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_PROFILES));
	}

	@Test
	@DisplayName("should retrieve empty change history for a profile")
	void emptyChangeHistoryForProfile() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/history", "konfigyr", service.slug(), "development")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(cursorModel(PropertyHistory.class))
				.extracting(CursorModel::getContent, InstanceOfAssertFactories.iterable(PropertyHistory.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to retrieve change history for an unknown profile")
	void changeHistoryForUnknownProfile() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/history", "konfigyr", service.slug(), "unknown-profile")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(profileNotFound("unknown-profile"));
	}

	@Test
	@DisplayName("should retrieve empty property history for a profile and revision")
	void emptyPropertyHistoryForRevision() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/property/{property}/history", "konfigyr", service.slug(), "development", "revision")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(collectionModel(ChangeHistoryRecord.class))
				.extracting(CollectionModel::getContent, InstanceOfAssertFactories.iterable(ChangeHistoryRecord.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to retrieve property history by revision for an unknown profile")
	void propertyHistoryForUnknownProfileByRevision() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/history/{revision}", "konfigyr", service.slug(), "unknown-profile", "revision")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(profileNotFound("unknown-profile"));
	}

	@Test
	@DisplayName("should fail to retrieve property history for profile for an unkonwn revision")
	void propertyHistoryForUnknownRevision() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/history/{revision}", "konfigyr", service.slug(), "locked", "unknown-revision")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(hasFailedWithException(RevisionNotFoundException.class))
				.satisfies(problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
						.hasTitle("Revision not found")
						.hasDetailContaining("We couldn't find a profile change history matching the requested revision hash")
				));
	}

	@Test
	@DisplayName("should retrieve empty property history for a profile and property name")
	void emptyPropertyHistoryForProperty() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/property/{property}/history", "konfigyr", service.slug(), "locked", "unknown.property.name")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(collectionModel(ChangeHistoryRecord.class))
				.extracting(CollectionModel::getContent, InstanceOfAssertFactories.iterable(ChangeHistoryRecord.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to retrieve property history by property name for an unknown profile")
	void propertyHistoryForUnknownProfileByPropertyName() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}/property/{property}/history", "konfigyr", service.slug(), "unknown-profile", "unknown.property.name")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(profileNotFound("unknown-profile"));
	}

	private Profile prepareServiceProfile(String name) {
		final var profile = assertThat(profiles.get(service, name))
				.as("Should find profile '%s' for service '%s'", name, service.id())
				.isPresent()
				.get()
				.actual();

		assertThatNoException()
				.as("Should create a new branch for profile '%s' without exceptions", profile.slug())
				.isThrownBy(() -> repository.create(profile));

		return profile;
	}

}
