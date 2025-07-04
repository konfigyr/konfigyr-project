package com.konfigyr.namespace.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkRelation;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.namespace.Member;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.support.Avatar;
import com.konfigyr.support.FullName;
import com.konfigyr.test.TestPrincipals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class MembersControllerTest extends AbstractNamespaceControllerTest {

	@Test
	@DisplayName("should retrieve members for namespace")
	void listMembers() {
		mvc.get().uri("/namespaces/{slug}/members", "john-doe")
				.queryParam("sort", "name")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(Member.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(1)
						.extracting(Member::id, Member::namespace, Member::account, Member::email, Member::role)
						.containsExactly(
								tuple(
										EntityId.from(1),
										EntityId.from(1),
										EntityId.from(1),
										"john.doe@konfigyr.com",
										NamespaceRole.ADMIN
								)
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
	@DisplayName("should fail to list members for namespace for non-members")
	void listMembersWithoutAccessRights()  {
		mvc.get().uri("/namespaces/{slug}/members", "john-doe")
				.queryParam("sort", "name")
				.with(authentication(TestPrincipals.jane(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to list members for namespace without namespaces:invite scope")
	void listMembersWithoutScope()  {
		mvc.get().uri("/namespaces/{slug}/members", "john-doe")
				.queryParam("sort", "name")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.INVITE_MEMBERS));
	}

	@Test
	@DisplayName("should retrieve member for namespace")
	void getMember() {
		mvc.get().uri("/namespaces/{slug}/members/{id}", "john-doe", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(Member.class)
				.returns(EntityId.from(1), Member::id)
				.returns(EntityId.from(1), Member::namespace)
				.returns(EntityId.from(1), Member::account)
				.returns("john.doe@konfigyr.com", Member::email)
				.returns(NamespaceRole.ADMIN, Member::role)
				.returns(FullName.of("John", "Doe"), Member::fullName)
				.returns(Avatar.generate(EntityId.from(1), "JD"), Member::avatar)
				.satisfies(it -> assertThat(it.since())
						.isCloseTo(OffsetDateTime.now().minusDays(5), within(20, ChronoUnit.SECONDS))
				);
	}

	@Test
	@DisplayName("should fail to retrieve unknown member")
	void getUnknownMember() {
		mvc.get().uri("/namespaces/{slug}/members/{id}", "konfigyr", EntityId.from(999).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(memberNotFound(999));
	}

	@Test
	@DisplayName("should fail to retrieve member for different namespace")
	void getMemberForDifferentNamespace() {
		mvc.get().uri("/namespaces/{slug}/members/{id}", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(memberNotFound(1));
	}

	@Test
	@DisplayName("should fail to retrieve member for unknown namespace")
	void getMemberForUnknownNamespace() {
		mvc.get().uri("/namespaces/{slug}/members/{id}", "unknown", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@DisplayName("should fail to retrieve members for namespace for non-members")
	void retrieveMembersWithoutAccessRights()  {
		mvc.get().uri("/namespaces/{slug}/members/{id}", "john-doe", EntityId.from(1).serialize())
				.queryParam("sort", "name")
				.with(authentication(TestPrincipals.jane(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to retrieve members for namespace without scope")
	void retrieveMembersWithoutScope()  {
		mvc.get().uri("/namespaces/{slug}/members/{id}", "john-doe", EntityId.from(1).serialize())
				.queryParam("sort", "name")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.INVITE_MEMBERS));
	}

	@Test
	@Transactional
	@DisplayName("should update member for namespace")
	void updateMember() {
		mvc.put().uri("/namespaces/{slug}/members/{id}", "konfigyr", EntityId.from(3).serialize())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"ADMIN\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(Member.class)
				.returns(EntityId.from(3), Member::id)
				.returns(EntityId.from(2), Member::namespace)
				.returns(EntityId.from(2), Member::account)
				.returns("jane.doe@konfigyr.com", Member::email)
				.returns(NamespaceRole.ADMIN, Member::role)
				.returns(FullName.of("Jane", "Doe"), Member::fullName)
				.returns(Avatar.generate(EntityId.from(2), "JD"), Member::avatar)
				.satisfies(it -> assertThat(it.since())
						.isCloseTo(OffsetDateTime.now().minusDays(2), within(20, ChronoUnit.SECONDS))
				);
	}

	@Test
	@DisplayName("should fail to update last administrator member to user")
	void updateLastAdminMember() {
		mvc.put().uri("/namespaces/{slug}/members/{id}", "konfigyr", EntityId.from(2).serialize())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"USER\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(operationNotSupported());
	}

	@Test
	@DisplayName("should fail to update unknown member")
	void updateUnknownMember() {
		mvc.put().uri("/namespaces/{slug}/members/{id}", "konfigyr", EntityId.from(999).serialize())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"USER\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(memberNotFound(999));
	}

	@Test
	@DisplayName("should fail to update member for different namespace")
	void updateMemberForDifferentNamespace() {
		mvc.put().uri("/namespaces/{slug}/members/{id}", "konfigyr", EntityId.from(1).serialize())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"USER\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(memberNotFound(1));
	}

	@Test
	@DisplayName("should fail to update member for unknown namespace")
	void updateMemberForUnknownNamespace() {
		mvc.put().uri("/namespaces/{slug}/members/{id}", "unknown-namespace", EntityId.from(1).serialize())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"USER\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to update member when not namespace administrator")
	void updateMemberForNonAdministrators() {
		mvc.put().uri("/namespaces/{slug}/members/{id}", "konfigyr", EntityId.from(2).serialize())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"USER\"}")
				.with(authentication(TestPrincipals.jane(), OAuthScope.INVITE_MEMBERS))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to update member when not namespaces:invite is missing")
	void updateMemberWithoutScope() {
		mvc.put().uri("/namespaces/{slug}/members/{id}", "konfigyr", EntityId.from(2).serialize())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"USER\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.DELETE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.INVITE_MEMBERS));
	}

	@Test
	@Transactional
	@DisplayName("should remove member for namespace")
	void removeMember() {
		mvc.delete().uri("/namespaces/{slug}/members/{id}", "konfigyr", EntityId.from(3).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	@Transactional
	@DisplayName("should fail to remove last admin member")
	void removeLastAdminMember() {
		mvc.delete().uri("/namespaces/{slug}/members/{id}", "john-doe", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(operationNotSupported());
	}

	@Test
	@DisplayName("should fail to remove unknown member")
	void removeUnknownMember() {
		mvc.delete().uri("/namespaces/{slug}/members/{id}", "konfigyr", EntityId.from(999).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(memberNotFound(999));
	}

	@Test
	@DisplayName("should fail to remove member for different namespace")
	void removeMemberForDifferentNamespace() {
		mvc.delete().uri("/namespaces/{slug}/members/{id}", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(memberNotFound(1));
	}

	@Test
	@DisplayName("should fail to remove member for unknown namespace")
	void removeMemberForUnknownNamespace() {
		mvc.delete().uri("/namespaces/{slug}/members/{id}", "unknown", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@DisplayName("should fail to remove member when not namespace administrator")
	void removeMemberForNonAdministrators() {
		mvc.delete().uri("/namespaces/{slug}/members/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.jane(), OAuthScope.NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to remove member when namespaces:invite scope is missing")
	void removeMemberWithoutScope() {
		mvc.delete().uri("/namespaces/{slug}/members/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.INVITE_MEMBERS));
	}

}
