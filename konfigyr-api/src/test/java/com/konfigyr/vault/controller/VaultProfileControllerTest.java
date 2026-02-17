package com.konfigyr.vault.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkRelation;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import com.konfigyr.vault.Profile;
import com.konfigyr.vault.ProfileNotFoundException;
import com.konfigyr.vault.ProfilePolicy;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class VaultProfileControllerTest extends AbstractControllerTest {

	@Test
	@DisplayName("should retrieve profiles for service")
	void listProfiles() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles", "konfigyr", "konfigyr-id")
				.queryParam("sort", "name")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(Profile.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(3)
						.satisfiesExactly(
								app -> assertThat(app)
										.returns(EntityId.from(1L), Profile::id)
										.returns("development", Profile::slug)
										.returns("Development", Profile::name)
										.returns(null, Profile::description)
										.returns(1, Profile::position)
										.returns(ProfilePolicy.UNPROTECTED, Profile::policy),
								app -> assertThat(app)
										.returns(EntityId.from(3L), Profile::id)
										.returns("production", Profile::slug)
										.returns("Prod", Profile::name)
										.returns("Careful!", Profile::description)
										.returns(3, Profile::position)
										.returns(ProfilePolicy.PROTECTED, Profile::policy),
								app -> assertThat(app)
										.returns(EntityId.from(2L), Profile::id)
										.returns("staging", Profile::slug)
										.returns("Staging", Profile::name)
										.returns(null, Profile::description)
										.returns(2, Profile::position)
										.returns(ProfilePolicy.PROTECTED, Profile::policy)
						)
				)
				.satisfies(it -> assertThat(it.getMetadata())
						.returns(20L, PagedModel.PageMetadata::size)
						.returns(0L, PagedModel.PageMetadata::number)
						.returns(3L, PagedModel.PageMetadata::totalElements)
						.returns(1L, PagedModel.PageMetadata::totalPages)
				)
				.satisfies(it -> assertThat(it.getLinks())
						.hasSize(2)
						.containsExactly(
								Link.of("http://localhost?page=1", LinkRelation.FIRST),
								Link.of("http://localhost?page=1", LinkRelation.LAST)
						)
				);
	}

	@Test
	@DisplayName("should retrieve profiles for service that are matching the search term")
	void searchProfiles() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles", "konfigyr", "konfigyr-id")
				.queryParam("term", "dev")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(Profile.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(1)
						.satisfiesExactly(
								app -> assertThat(app)
										.returns(EntityId.from(1L), Profile::id)
										.returns("development", Profile::slug)
										.returns("Development", Profile::name)
										.returns(null, Profile::description)
										.returns(ProfilePolicy.UNPROTECTED, Profile::policy)
						)
				)
				.satisfies(it -> assertThat(it.getMetadata())
						.returns(20L, PagedModel.PageMetadata::size)
						.returns(0L, PagedModel.PageMetadata::number)
						.returns(1L, PagedModel.PageMetadata::totalElements)
						.returns(1L, PagedModel.PageMetadata::totalPages)
				)
				.satisfies(it -> assertThat(it.getLinks())
						.hasSize(2)
						.containsExactly(
								Link.of("http://localhost?page=1", LinkRelation.FIRST),
								Link.of("http://localhost?page=1", LinkRelation.LAST)
						)
				);
	}

	@Test
	@DisplayName("should not retrieve profiles from an unknown namespace")
	void retrieveProfilesFromUnknownNamespace() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles", "unknown-namespace", "konfigyr-id")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should not retrieve profiles from an unknown service")
	void retrieveProfilesFromUnknownService() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles", "konfigyr", "unknown-service")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should not retrieve profiles when user is not a member of a namespace")
	void retrieveProfilesWithoutMembership() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles", "john-doe", "john-doe-blog")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should perform a service profile check by name on an existing profile")
	void shouldCheckExistingProfile() {
		mvc.head().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "konfigyr", "konfigyr-id", "staging")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.body()
				.isEmpty();
	}

	@Test
	@DisplayName("should perform a service profile check by slug on an unknown profile")
	void shouldCheckUnknownProfile() {
		mvc.head().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "konfigyr", "konfigyr-id", "live")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.body()
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve service profile by entity identifier")
	void retrieveProfile() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "konfigyr", "konfigyr-id", "staging")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(Profile.class)
				.returns(EntityId.from(2L), Profile::id)
				.returns("staging", Profile::slug)
				.returns("Staging", Profile::name)
				.returns(null, Profile::description)
				.returns(ProfilePolicy.PROTECTED, Profile::policy)
				.returns(2, Profile::position)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now().minusDays(2), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should not retrieve a service profile that belongs to a different service")
	void retrieveProfileForDifferentService() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "konfigyr", "konfigyr-id", "live")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(profileNotFound("live"));
	}

	@Test
	@DisplayName("should not retrieve an unknown service profile")
	void retrieveUnknownProfile() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "konfigyr", "konfigyr-id", "unknown")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(profileNotFound("unknown"));
	}

	@Test
	@DisplayName("should not retrieve a service profile an unknown service")
	void retrieveProfileForUnknownService() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "konfigyr", "unknown", "staging")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown"));
	}

	@Test
	@DisplayName("should not retrieve a service profile an unknown namespace")
	void retrieveProfileForUnknownNamespace() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "unknown", "konfigyr-id", "staging")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@DisplayName("should not retrieve service profile when user is not a member of a namespace")
	void retrieveProfileWithoutMembership() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "john-doe", "john-doe-blog", "live")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should not retrieve service profile without required scope")
	void retrieveProfileWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles", "john-doe", "john-doe-blog", "live")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_PROFILES));
	}

	@Test
	@Transactional
	@DisplayName("should create service profile")
	void createProfile() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/profiles", "konfigyr", "konfigyr-id")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"integration\", \"name\":\"Integration\", \"policy\":\"PROTECTED\", \"description\":\"For integration tests\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(Profile.class)
				.returns("integration", Profile::slug)
				.returns("Integration", Profile::name)
				.returns("For integration tests", Profile::description)
				.returns(ProfilePolicy.PROTECTED, Profile::policy)
				.returns(1, Profile::position)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should fail to create service profile with invalid data")
	void createProfileWithInvalidPayload() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/profiles", "konfigyr", "konfigyr-id")
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
								.containsExactlyInAnyOrder("policy", "name", "slug")
						)
				));
	}

	@Test
	@DisplayName("should not create a service profile an unknown service")
	void createProfileForUnknownService() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/profiles", "konfigyr", "unknown-service")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"integration\", \"name\":\"Integration\", \"policy\":\"PROTECTED\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should not create a service profile an unknown namespace")
	void createProfileForUnknownNamespace() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/profiles", "unknown", "konfigyr-id")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"integration\", \"name\":\"Integration\", \"policy\":\"PROTECTED\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@DisplayName("should not create service profile when user is not a member of a namespace")
	void createProfileWithoutMembership() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/profiles", "john-doe", "john-doe-blog")
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"integration\", \"name\":\"Integration\", \"policy\":\"PROTECTED\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should not create service profile without required scope")
	void createProfileWithoutScope() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/profiles", "john-doe", "john-doe-blog")
				.with(authentication(TestPrincipals.john()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"integration\", \"name\":\"Integration\", \"policy\":\"PROTECTED\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_PROFILES));
	}

	@Test
	@Transactional
	@DisplayName("should update service profile")
	void updateProfile() {
		mvc.put().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "konfigyr", "konfigyr-id", "development")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Dev\", \"policy\":\"PROTECTED\", \"description\":\"Protect yourself!\", \"position\": 10}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(Profile.class)
				.returns("development", Profile::slug)
				.returns("Dev", Profile::name)
				.returns("Protect yourself!", Profile::description)
				.returns(10, Profile::position)
				.returns(ProfilePolicy.PROTECTED, Profile::policy)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now().minusDays(5), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@Transactional
	@DisplayName("should update service profile with empty data")
	void updateProfileWithEmptyPayload() {
		mvc.put().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "konfigyr", "konfigyr-id", "development")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.bodyJson()
				.convertTo(Profile.class)
				.returns("development", Profile::slug)
				.returns("Development", Profile::name)
				.returns(null, Profile::description)
				.returns(ProfilePolicy.UNPROTECTED, Profile::policy)
				.returns(1, Profile::position)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now().minusDays(5), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should not update an unknown service profile")
	void updateUnknownProfile() {
		mvc.put().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "konfigyr", "konfigyr-id", "live")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"policy\":\"PROTECTED\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(profileNotFound("live"));
	}

	@Test
	@DisplayName("should not update a service profile an unknown service")
	void updateProfileForUnknownService() {
		mvc.put().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "konfigyr", "unknown-service", "live")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"policy\":\"PROTECTED\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should not update a service profile an unknown namespace")
	void updateProfileForUnknownNamespace() {
		mvc.put().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "unknown", "konfigyr-id", "live")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"policy\":\"PROTECTED\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@DisplayName("should not update service profile when user is not a member of a namespace")
	void updateProfileWithoutMembership() {
		mvc.put().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "john-doe", "john-doe-blog", "live")
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"policy\":\"PROTECTED\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should not update service profile without required scope")
	void updateProfileWithoutScope() {
		mvc.put().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "john-doe", "john-doe-blog", "live")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"policy\":\"PROTECTED\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_PROFILES));
	}

	@Test
	@Transactional
	@DisplayName("should delete service profile")
	void deleteProfile() {
		mvc.delete().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "konfigyr", "konfigyr-id", "development")
				.with(authentication(TestPrincipals.john(), OAuthScope.DELETE_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NO_CONTENT);

		mvc.get().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "konfigyr", "konfigyr-id", "development")
				.with(authentication(TestPrincipals.john(), OAuthScope.DELETE_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(profileNotFound("development"));
	}

	@Test
	@DisplayName("should not delete an unknown service profile")
	void deleteUnknownProfile() {
		mvc.delete().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "konfigyr", "konfigyr-id", "live")
				.with(authentication(TestPrincipals.john(), OAuthScope.DELETE_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(profileNotFound("live"));
	}

	@Test
	@DisplayName("should not delete a service profile an unknown service")
	void deleteProfileForUnknownService() {
		mvc.delete().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "konfigyr", "unknown-service", "live")
				.with(authentication(TestPrincipals.john(), OAuthScope.DELETE_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should not delete a service profile an unknown namespace")
	void deleteProfileForUnknownNamespace() {
		mvc.delete().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "unknown", "konfigyr-id", "live")
				.with(authentication(TestPrincipals.john(), OAuthScope.DELETE_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@DisplayName("should not delete service profile when user is not a member of a namespace")
	void deleteProfileWithoutMembership() {
		mvc.delete().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "john-doe", "john-doe-blog", "live")
				.with(authentication(TestPrincipals.jane(), OAuthScope.DELETE_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should not delete service profile without required scope")
	void deleteProfileWithoutScope() {
		mvc.delete().uri("/namespaces/{slug}/services/{service}/profiles/{profile}", "john-doe", "john-doe-blog", "live")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.DELETE_PROFILES));
	}

	static Consumer<MvcTestResult> profileNotFound(String profile) {
		return problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
				.hasTitle("Profile not found")
				.hasDetailContaining("We couldn't find a profile matching your request.")
		).andThen(hasFailedWithException(ProfileNotFoundException.class, ex -> ex
				.hasMessageContaining("Could not find a profile with the following name: %s", profile)
		));
	}

}
