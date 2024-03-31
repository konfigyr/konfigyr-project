package com.konfigyr.security;

import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Vladimir Spasic
 **/
@TestProfile
@SpringBootTest
@AutoConfigureMockMvc
@ImportTestcontainers(TestContainers.class)
class SecurityIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Test
	@DisplayName("should redirect to login page when not authenticated")
	void shouldRedirectToLogin() throws Exception {
		mvc.perform(get("/"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("http://localhost/login"));
	}

	@Test
	@DisplayName("should render OAuth2 providers on login page")
	void shouldRenderProviders() throws Exception {
		mvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(
						containsStringIgnoringCase("<a href=\"/oauth2/authorization/github\">")
				));
	}

}
