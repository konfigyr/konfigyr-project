package com.konfigyr.artifactory.controller;

import com.konfigyr.artifactory.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class PublicationsControllerTest extends AbstractControllerTest {

	@Test
	@DisplayName("should list artifacts owned by the namespace")
	void shouldListOwnedArtifacts() {
		mvc.get().uri("/namespaces/{namespace}/artifacts", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(pagedModel(ArtifactDefinition.class))
				.satisfies(it -> assertThat(it.getContent())
						.extracting(ArtifactDefinition::id)
						.containsExactlyInAnyOrder(
								EntityId.from(2), EntityId.from(3), EntityId.from(4), EntityId.from(5),
								EntityId.from(6), EntityId.from(7), EntityId.from(8), EntityId.from(9),
								EntityId.from(10), EntityId.from(11), EntityId.from(12), EntityId.from(14)
						));
	}

	@Test
	@DisplayName("should list artifacts filtered by groupId query parameter")
	void shouldListArtifactsFilteredByGroupIdQueryParam() {
		mvc.get().uri("/namespaces/{namespace}/artifacts?groupId=org.springframework.modulith", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(pagedModel(ArtifactDefinition.class))
				.satisfies(it -> assertThat(it.getContent())
						.extracting(ArtifactDefinition::id)
						.containsExactlyInAnyOrder(EntityId.from(9), EntityId.from(10)));
	}

	@Test
	@DisplayName("should list artifacts filtered by groupId path segment")
	void shouldListArtifactsByGroupIdPathSegment() {
		mvc.get().uri("/namespaces/{namespace}/artifacts/{groupId}", "konfigyr", "org.springframework.modulith")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(pagedModel(ArtifactDefinition.class))
				.satisfies(it -> assertThat(it.getContent())
						.extracting(ArtifactDefinition::id)
						.containsExactlyInAnyOrder(EntityId.from(9), EntityId.from(10)));
	}

	@Test
	@DisplayName("should forbid listing artifacts for a namespace the caller is not a member of")
	void shouldForbidListForNonMember() {
		mvc.get().uri("/namespaces/{namespace}/artifacts", "john-doe")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should forbid listing artifacts without the read-artifacts scope")
	void shouldForbidListWithoutScope() {
		mvc.get().uri("/namespaces/{namespace}/artifacts", "konfigyr")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_ARTIFACTS));
	}

	@Test
	@DisplayName("should retrieve an artifact definition owned by the namespace")
	void shouldGetOwnedArtifactDefinition() {
		mvc.get().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}", "konfigyr", "com.konfigyr", "konfigyr-api")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ArtifactDefinition.class)
				.returns(EntityId.from(5), ArtifactDefinition::id)
				.returns(Owners.konfigyr(), ArtifactDefinition::owner)
				.returns(ArtifactVisibility.PUBLIC, ArtifactDefinition::visibility);
	}

	@Test
	@DisplayName("should never retrieve an artifact definition owned by a different namespace, even when public")
	void shouldFailToGetArtifactDefinitionOwnedByDifferentNamespace() {
		mvc.get().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}", "john-doe", "com.konfigyr", "konfigyr-crypto-api")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(hasFailedWithException(ArtifactDefinitionNotFoundException.class));
	}

	@Test
	@DisplayName("should fail to retrieve an unknown artifact definition")
	void shouldFailToGetUnknownArtifactDefinition() {
		mvc.get().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}", "konfigyr", "com.konfigyr", "does-not-exist")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(hasFailedWithException(ArtifactDefinitionNotFoundException.class));
	}

	@Test
	@DisplayName("should forbid retrieving an artifact definition for a namespace the caller is not a member of")
	void shouldForbidGetForNonMember() {
		mvc.get().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}", "john-doe", "doe.john", "website")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should perform an existence check for an artifact owned by the namespace")
	void shouldCheckExistingOwnedArtifact() {
		mvc.head().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}", "konfigyr", "com.konfigyr", "konfigyr-api")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.body()
				.isEmpty();
	}

	@Test
	@DisplayName("should perform an existence check for an artifact owned by a different namespace, even when public")
	void shouldCheckNonExistentArtifactOwnedByDifferentNamespace() {
		mvc.head().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}", "john-doe", "com.konfigyr", "konfigyr-crypto-api")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.body()
				.isEmpty();
	}

	@Test
	@DisplayName("should forbid the existence check for a namespace the caller is not a member of")
	void shouldForbidExistsForNonMember() {
		mvc.head().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}", "john-doe", "doe.john", "website")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	@Transactional
	@DisplayName("should change the visibility of an owned artifact")
	void shouldChangeVisibilityOfOwnedArtifact() {
		mvc.put().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/visibility", "konfigyr", "com.konfigyr", "konfigyr-internal-secrets")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_ARTIFACTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(new PublicationsController.ChangeVisibilityRequest(ArtifactVisibility.PUBLIC)))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NO_CONTENT);

		mvc.get().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}", "konfigyr", "com.konfigyr", "konfigyr-internal-secrets")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.bodyJson()
				.convertTo(ArtifactDefinition.class)
				.returns(ArtifactVisibility.PUBLIC, ArtifactDefinition::visibility);
	}

	@Test
	@DisplayName("should fail to change visibility of an unknown artifact")
	void shouldFailToChangeVisibilityOfUnknownArtifact() {
		mvc.put().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/visibility", "konfigyr", "com.konfigyr", "does-not-exist")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_ARTIFACTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(new PublicationsController.ChangeVisibilityRequest(ArtifactVisibility.PUBLIC)))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(hasFailedWithException(ArtifactDefinitionNotFoundException.class));
	}

	@Test
	@DisplayName("should forbid changing visibility for a member that is not an admin of the namespace")
	void shouldForbidChangeVisibilityForNonAdminMember() {
		mvc.put().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/visibility", "konfigyr", "com.konfigyr", "konfigyr-internal-secrets")
				.with(authentication(TestPrincipals.jane(), OAuthScope.PUBLISH_ARTIFACTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(new PublicationsController.ChangeVisibilityRequest(ArtifactVisibility.PUBLIC)))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should forbid changing visibility without the publish-artifacts scope")
	void shouldForbidChangeVisibilityWithoutScope() {
		mvc.put().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/visibility", "konfigyr", "com.konfigyr", "konfigyr-internal-secrets")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsBytes(new PublicationsController.ChangeVisibilityRequest(ArtifactVisibility.PUBLIC)))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.PUBLISH_ARTIFACTS));
	}

	@Test
	@Transactional
	@DisplayName("should deregister an owned artifact together with all of its versions")
	void shouldDeregisterOwnedArtifact() {
		mvc.delete().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}", "konfigyr", "com.konfigyr", "konfigyr-crypto-tink")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NO_CONTENT);

		mvc.head().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}", "konfigyr", "com.konfigyr", "konfigyr-crypto-tink")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	@DisplayName("should fail to deregister an unknown artifact")
	void shouldFailToDeregisterUnknownArtifact() {
		mvc.delete().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}", "konfigyr", "com.konfigyr", "does-not-exist")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(hasFailedWithException(ArtifactDefinitionNotFoundException.class));
	}

	@Test
	@DisplayName("should forbid deregistering an artifact for a member that is not an admin of the namespace")
	void shouldForbidDeregisterForNonAdminMember() {
		mvc.delete().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}", "konfigyr", "com.konfigyr", "konfigyr-crypto-tink")
				.with(authentication(TestPrincipals.jane(), OAuthScope.PUBLISH_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should search artifact versions owned by the namespace")
	void shouldSearchOwnedArtifactVersions() {
		mvc.get().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/versions", "konfigyr", "com.konfigyr", "konfigyr-crypto-api")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(pagedModel(VersionedArtifact.class))
				.satisfies(it -> assertThat(it.getContent())
						.extracting(VersionedArtifact::id)
						.containsExactlyInAnyOrder(EntityId.from(2), EntityId.from(3), EntityId.from(4)));
	}

	@Test
	@DisplayName("should filter searched artifact versions by version query parameter")
	void shouldSearchOwnedArtifactVersionsFilteredByVersion() {
		mvc.get().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/versions?version=1.0.1", "konfigyr", "com.konfigyr", "konfigyr-crypto-api")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(pagedModel(VersionedArtifact.class))
				.satisfies(it -> assertThat(it.getContent())
						.extracting(VersionedArtifact::id)
						.containsExactly(EntityId.from(3)));
	}

	@Test
	@DisplayName("should fail to search versions for an unknown artifact")
	void shouldFailToSearchVersionsForUnknownArtifact() {
		mvc.get().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/versions", "konfigyr", "com.konfigyr", "does-not-exist")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(hasFailedWithException(ArtifactDefinitionNotFoundException.class));
	}

	@Test
	@DisplayName("should forbid searching versions for a namespace the caller is not a member of")
	void shouldForbidVersionsForNonMember() {
		mvc.get().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/versions", "john-doe", "doe.john", "website")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should retrieve an artifact version owned by the namespace")
	void shouldGetOwnedArtifactVersion() {
		mvc.get().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/{version}", "konfigyr", "com.konfigyr", "konfigyr-crypto-api", "1.0.0")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(VersionedArtifact.class)
				.returns(EntityId.from(2), VersionedArtifact::id)
				.returns(Owners.konfigyr(), VersionedArtifact::owner);
	}

	@Test
	@DisplayName("should fail to retrieve an unknown artifact version")
	void shouldFailToGetUnknownArtifactVersion() {
		mvc.get().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/{version}", "konfigyr", "com.konfigyr", "konfigyr-crypto-api", "99.0.0")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(hasFailedWithException(ArtifactVersionNotFoundException.class));
	}

	@Test
	@DisplayName("should forbid retrieving an artifact version for a namespace the caller is not a member of")
	void shouldForbidGetVersionForNonMember() {
		mvc.get().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/{version}", "john-doe", "doe.john", "website", "1.0.0")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should perform an existence check for an artifact version owned by the namespace")
	void shouldCheckExistingOwnedArtifactVersion() {
		mvc.head().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/{version}", "konfigyr", "com.konfigyr", "konfigyr-crypto-api", "1.0.0")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.body()
				.isEmpty();
	}

	@Test
	@DisplayName("should perform an existence check for an unknown artifact version")
	void shouldCheckNonExistentArtifactVersion() {
		mvc.head().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/{version}", "konfigyr", "com.konfigyr", "konfigyr-crypto-api", "99.0.0")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.body()
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should retract a single owned artifact version, leaving the definition and other versions untouched")
	void shouldRetractOwnedVersion() {
		mvc.delete().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/{version}", "konfigyr", "org.springframework.boot", "spring-boot-jooq", "4.0.4")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NO_CONTENT);

		mvc.head().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/{version}", "konfigyr", "org.springframework.boot", "spring-boot-jooq", "4.0.4")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.hasStatus(HttpStatus.NOT_FOUND);

		mvc.head().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}", "konfigyr", "org.springframework.boot", "spring-boot-jooq")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_ARTIFACTS))
				.exchange()
				.assertThat()
				.hasStatusOk();
	}

	@Test
	@DisplayName("should fail to retract an unknown artifact version")
	void shouldFailToRetractUnknownVersion() {
		mvc.delete().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/{version}", "konfigyr", "com.konfigyr", "konfigyr-crypto-api", "99.0.0")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(hasFailedWithException(ArtifactVersionNotFoundException.class));
	}

	@Test
	@DisplayName("should forbid retracting an artifact version for a member that is not an admin of the namespace")
	void shouldForbidRetractForNonAdminMember() {
		mvc.delete().uri("/namespaces/{namespace}/artifacts/{groupId}/{artifactId}/{version}", "konfigyr", "com.konfigyr", "konfigyr-crypto-api", "1.0.0")
				.with(authentication(TestPrincipals.jane(), OAuthScope.PUBLISH_ARTIFACTS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

}
