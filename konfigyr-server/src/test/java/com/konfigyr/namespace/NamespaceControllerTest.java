package com.konfigyr.namespace;

import com.konfigyr.assertions.AssertMatcher;
import com.konfigyr.entity.EntityId;
import com.konfigyr.registry.Repository;
import com.konfigyr.test.TestAccounts;
import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestPrincipals;
import com.konfigyr.test.TestProfile;
import jakarta.servlet.ServletException;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestProfile
@SpringBootTest
@AutoConfigureMockMvc
@ImportTestcontainers(TestContainers.class)
class NamespaceControllerTest {

	@Autowired Invitations invitations;
	@Autowired NamespaceManager namespaces;

	static MockMvc mvc;

	@BeforeAll
	static void setup(WebApplicationContext context) {
		mvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@Test
	@DisplayName("should render namespace details")
	void shouldRenderNamespaceDetails() throws Exception {
		mvc.perform(get("/namespace/konfigyr"))
				.andDo(log())
				.andExpect(status().isOk())
				.andExpect(view().name("namespaces/details"))
				.andExpect(model().attribute("namespace", AssertMatcher.of(namespace -> assertThat(namespace)
						.isNotNull()
						.isInstanceOf(Namespace.class)
						.asInstanceOf(InstanceOfAssertFactories.type(Namespace.class))
						.returns(EntityId.from(2), Namespace::id)
						.returns("konfigyr", Namespace::slug)
						.returns("Konfigyr", Namespace::name)
						.returns(NamespaceType.TEAM, Namespace::type)
				)))
				.andExpect(model().attribute("repositories", AssertMatcher.of(repositories -> assertThat(repositories)
						.isNotNull()
						.isInstanceOf(Page.class)
						.asInstanceOf(InstanceOfAssertFactories.iterable(Repository.class))
						.extracting(Repository::id)
						.containsExactlyInAnyOrder(EntityId.from(2), EntityId.from(3))
				)));
	}

	@Test
	@DisplayName("should render namespace members page")
	void shouldRenderNamespaceMembers() throws Exception {
		mvc.perform(get("/namespace/konfigyr/members"))
				.andDo(log())
				.andExpect(status().isOk())
				.andExpect(view().name("namespaces/members"))
				.andExpect(model().attribute("namespace", AssertMatcher.of(namespace -> assertThat(namespace)
						.isNotNull()
						.isInstanceOf(Namespace.class)
						.asInstanceOf(InstanceOfAssertFactories.type(Namespace.class))
						.returns(EntityId.from(2), Namespace::id)
						.returns("konfigyr", Namespace::slug)
						.returns("Konfigyr", Namespace::name)
						.returns(NamespaceType.TEAM, Namespace::type)
				)))
				.andExpect(model().attribute("members", AssertMatcher.of(members -> assertThat(members)
						.isNotNull()
						.isInstanceOf(Page.class)
						.asInstanceOf(InstanceOfAssertFactories.iterable(Member.class))
						.extracting(Member::id)
						.containsExactlyInAnyOrder(EntityId.from(2), EntityId.from(3))
				)));
	}

	@Test
	@Transactional
	@DisplayName("should update namespace members role")
	void shouldUpdateNamespaceMemberRole() throws Exception {
		var request = post("/namespace/konfigyr/members/update")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("member", EntityId.from(2).serialize())
				.param("role", NamespaceRole.USER.name())
				.with(csrf());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/namespace/konfigyr/members"))
				.andExpect(flash().attribute("notification", AssertMatcher.of(notification -> assertThat(notification)
						.asString()
						.containsIgnoringCase("member was successfully updated")
				)));

		assertThat(namespaces.findMembers("konfigyr"))
				.filteredOn(it -> EntityId.from(2).equals(it.id()))
				.extracting(Member::role)
				.containsExactly(NamespaceRole.USER);
	}

	@Test
	@Transactional
	@DisplayName("should remove namespace member")
	void shouldRemoveNamespaceMember() throws Exception {
		var request = post("/namespace/konfigyr/members/remove")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("member", EntityId.from(2).serialize())
				.with(csrf());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/namespace/konfigyr/members"))
				.andExpect(flash().attribute("notification", AssertMatcher.of(notification -> assertThat(notification)
						.asString()
						.containsIgnoringCase("member was successfully removed")
				)));

		assertThat(namespaces.findMembers("konfigyr"))
				.filteredOn(it -> EntityId.from(2).equals(it.id()))
				.isEmpty();
	}

	@Test
	@DisplayName("should render namespace invitations page")
	void shouldRenderNamespaceInvitations() throws Exception {
		mvc.perform(get("/namespace/konfigyr/members/invitations"))
				.andDo(log())
				.andExpect(status().isOk())
				.andExpect(view().name("namespaces/invitations"))
				.andExpect(model().attribute("namespace", AssertMatcher.of(namespace -> assertThat(namespace)
						.isNotNull()
						.isInstanceOf(Namespace.class)
						.asInstanceOf(InstanceOfAssertFactories.type(Namespace.class))
						.returns(EntityId.from(2), Namespace::id)
						.returns("konfigyr", Namespace::slug)
						.returns("Konfigyr", Namespace::name)
						.returns(NamespaceType.TEAM, Namespace::type)
				)))
				.andExpect(model().attribute("invitations", AssertMatcher.of(invitations -> assertThat(invitations)
						.isNotNull()
						.isInstanceOf(Page.class)
						.asInstanceOf(InstanceOfAssertFactories.iterable(Invitation.class))
						.isEmpty()
				)));
	}

	@Test
	@Transactional
	@DisplayName("should send invite to new member")
	void shouldSendInvite() throws Exception {
		var request = post("/namespace/konfigyr/members")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("email", "muad.dib@konfigyr.com")
				.param("role", NamespaceRole.USER.name())
				.with(authentication(TestPrincipals.john()))
				.with(csrf());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/namespace/konfigyr/members"))
				.andExpect(flash().attribute("success", AssertMatcher.of(message -> assertThat(message)
						.asString()
						.contains("muad.dib@konfigyr.com")
				)));

		final var namespace = namespaces.findBySlug("konfigyr").orElseThrow();
		final var invitations = this.invitations.find(namespace, Pageable.unpaged());

		assertThat(invitations)
				.hasSize(1)
				.extracting("sender.email", "recipient.email", "role")
				.containsExactly(tuple(TestAccounts.john().build().email(), "muad.dib@konfigyr.com", NamespaceRole.USER));

		// make sure that invitation is also rendered in the invitations page
		mvc.perform(get("/namespace/konfigyr/members/invitations"))
				.andDo(log())
				.andExpect(status().isOk())
				.andExpect(view().name("namespaces/invitations"))
				.andExpect(model().attribute("invitations", AssertMatcher.of(it -> assertThat(it)
						.isNotNull()
						.isInstanceOf(Page.class)
						.asInstanceOf(InstanceOfAssertFactories.iterable(Invitation.class))
						.containsAll(invitations)
				)));

		final Invitation invitation = invitations.stream().findFirst().orElseThrow();

		// make sure that invitation page works with the generated key
		mvc.perform(get("/namespace/konfigyr/members/invitation/{key}", invitation.key()))
				.andDo(log())
				.andExpect(status().isOk())
				.andExpect(view().name("namespaces/invitation"))
				.andExpect(model().attribute("namespace", namespace))
				.andExpect(model().attribute("invitation", invitation));
	}

	@Test
	@Transactional
	@DisplayName("should render an empty invitation page when key is expired")
	void shouldRenderEmptyInvitationPage() throws Exception {
		mvc.perform(get("/namespace/konfigyr/members/invitation/expired-key"))
				.andDo(log())
				.andExpect(status().isOk())
				.andExpect(view().name("namespaces/invitation"))
				.andExpect(model().attribute("namespace", AssertMatcher.of(namespace -> assertThat(namespace)
						.isNotNull()
						.isInstanceOf(Namespace.class)
						.asInstanceOf(InstanceOfAssertFactories.type(Namespace.class))
						.returns(EntityId.from(2), Namespace::id)
						.returns("konfigyr", Namespace::slug)
						.returns("Konfigyr", Namespace::name)
						.returns(NamespaceType.TEAM, Namespace::type)
				)))
				.andExpect(model().attributeDoesNotExist("invitation"));
	}

	@Test
	@DisplayName("should throw namespace not found for for unknown namespaces when rendering details page")
	void shouldThrowNamespaceNotFoundForUnknownNamespaces() {
		assertThatThrownBy(() -> mvc.perform(get("/namespace/unknown")))
				.isInstanceOf(ServletException.class)
				.hasCauseInstanceOf(NamespaceNotFoundException.class);
	}

	@Test
	@DisplayName("should throw namespace not found for invalid slugs when rendering details page")
	void shouldThrowNamespaceNotFoundForInvalidSlugs() {
		assertThatThrownBy(() -> mvc.perform(get("/namespace/{slug}", RandomStringUtils.randomAlphanumeric(300))))
				.isInstanceOf(ServletException.class)
				.hasCauseInstanceOf(NamespaceNotFoundException.class);
	}

	@Test
	@DisplayName("should check for an available namespace name")
	void shouldCheckForAvailableNamespace() throws Exception {
		final var request = post("/namespaces/check-name")
				.queryParam("value", "available-namespace")
				.with(csrf());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isOk())
				.andExpect(view().name("namespaces/check-name"))
				.andExpect(model().attribute("slug", "available-namespace"))
				.andExpect(model().attribute("unavailable", false))
				.andExpect(model().attribute("valid", true));
	}

	@Test
	@DisplayName("should check for an unavailable namespace name")
	void shouldCheckForUnavailableNamespace() throws Exception {
		final var request = post("/namespaces/check-name")
				.queryParam("value", "Konfigyr")
				.with(csrf());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isUnprocessableEntity())
				.andExpect(view().name("namespaces/check-name"))
				.andExpect(model().attribute("slug", "konfigyr"))
				.andExpect(model().attribute("unavailable", true))
				.andExpect(model().attribute("valid", false));
	}

	@Test
	@DisplayName("should check for an invalid namespace name")
	void shouldCheckForInvalidNamespaceName() throws Exception {
		final var name = RandomStringUtils.randomAlphanumeric(300);

		final var request = post("/namespaces/check-name")
				.queryParam("value", name)
				.with(csrf());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isOk())
				.andExpect(view().name("namespaces/check-name"))
				.andExpect(model().attribute("slug", name))
				.andExpect(model().attribute("unavailable", false))
				.andExpect(model().attribute("valid", true));
	}

	@Test
	@DisplayName("should fail to check namespace name without CSRF token")
	void shouldNotCheckWhenMissingCSRFToken() throws Exception {
		final var request = post("/namespaces/check-name")
				.queryParam("value", "csrf-missing");

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("should fail to check namespace name with invalid CSRF token")
	void shouldNotCheckWithInvalidCSRFToken() throws Exception {
		final var request = post("/namespaces/check-name")
				.queryParam("value", "csrf-invalid")
				.with(csrf().useInvalidToken());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isForbidden());
	}

}
