package com.konfigyr.namespace;

import com.konfigyr.NamespaceTestConfiguration;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = NamespaceTestConfiguration.class)
class NamespaceControllerTest {

	static MockMvc mvc;

	@BeforeAll
	static void setup(WebApplicationContext context) {
		mvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
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
