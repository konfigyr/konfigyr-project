package com.konfigyr.security.provision;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountManager;
import com.konfigyr.account.AccountStatus;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.NamespaceType;
import com.konfigyr.test.AbstractIntegrationTest;
import jakarta.servlet.DispatcherType;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.web.WebAttributes;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProvisioningIntegrationTest extends AbstractIntegrationTest {

	static MockMvc mvc;

	@Autowired
	AccountManager accountManager;

	@Autowired
	NamespaceManager namespaceManager;

	@BeforeAll
	static void setup(WebApplicationContext context) {
		mvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@Test
	@DisplayName("should render provisioning page")
	void shouldRenderProvisioningPage() throws Exception {
		var request = get(ProvisioningRedirectFilter.DEFAULT_PROVISIONING_URL)
				.sessionAttr(WebAttributes.AUTHENTICATION_EXCEPTION, new ProvisioningRequiredException());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isOk())
				.andExpect(view().name("accounts/provision"))
				.andExpect(model().attribute("form", new ProvisioningForm()));
	}

	@Test
	@DisplayName("should redirect to login page when provisioning required exception is not present")
	void shouldRedirectToLogin() throws Exception {
		var request = get(ProvisioningRedirectFilter.DEFAULT_PROVISIONING_URL);

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	@DisplayName("should validate provisioning form")
	void shouldValidateProvisioningForm() throws Exception {
		var request = post(ProvisioningRedirectFilter.DEFAULT_PROVISIONING_URL)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.with(csrf());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isBadRequest())
				.andExpect(view().name("accounts/provision"))
				.andExpect(model().attributeErrorCount("form", 3))
				.andExpect(model().attributeHasFieldErrorCode("form", "email", "NotEmpty"))
				.andExpect(model().attributeHasFieldErrorCode("form", "namespace", "NotEmpty"))
				.andExpect(model().attributeHasFieldErrorCode("form", "type", "NotNull"));
	}

	@Test
	@Transactional
	@DisplayName("should provision account and namespace")
	void shouldProvisionAccountAndNamespace() throws Exception {
		var request = post(ProvisioningRedirectFilter.DEFAULT_PROVISIONING_URL)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("email", "muad.dib@atreides.com")
				.param("namespace", "Atreides")
				.param("type", NamespaceType.TEAM.name())
				.param("firstName", "Muad Dib")
				.param("lastName", "Atreides")
				.with(csrf());

		final var result = mvc.perform(request)
				.andDo(log())
				.andExpect(status().isOk())
				.andExpect(forwardedUrlPattern(ProvisioningAuthenticationFilter.DEFAULT_PROCESSING_URL + "?account=*"))
				.andReturn();

		final var account = accountManager.findByEmail("muad.dib@atreides.com")
				.orElseThrow(() -> new IllegalStateException("Account was not created"));

		assertThat(account)
				.returns("muad.dib@atreides.com", Account::email)
				.returns(AccountStatus.ACTIVE, Account::status)
				.returns("Muad Dib", Account::firstName)
				.returns("Atreides", Account::lastName);

		assertThat(namespaceManager.findBySlug("atreides"))
				.isNotEmpty()
				.get()
				.returns("Atreides", Namespace::name)
				.returns(NamespaceType.TEAM, Namespace::type)
				.returns(null, Namespace::description);

		assertThat(result.getResponse().getForwardedUrl())
				.isNotNull()
				.contains("?account=" + account.id().serialize());

		final var forward = get(result.getResponse().getForwardedUrl())
				.with(ProvisioningConfigurerTest.dispatcherType(DispatcherType.FORWARD));

		mvc.perform(forward)
				.andDo(log())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"))
				.andExpect(authenticated().withAuthenticationName(account.id().serialize()));
	}

	@Test
	@DisplayName("should render provisioning page when account already exists")
	void shouldRenderAccountExistsError() throws Exception {
		var request = post(ProvisioningRedirectFilter.DEFAULT_PROVISIONING_URL)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("email", "john.doe@konfigyr.com")
				.param("namespace", "john.doe")
				.param("type", NamespaceType.PERSONAL.name())
				.param("firstName", "John")
				.param("lastName", "Doe")
				.with(csrf());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isBadRequest())
				.andExpect(view().name("accounts/provision"))
				.andExpect(model().attributeErrorCount("form", 1))
				.andExpect(model().attributeHasFieldErrorCode("form", "email", "errors.account.exists"));
	}

	@Test
	@DisplayName("should render provisioning page when namespace already exists")
	void shouldRenderNamespaceExistsError() throws Exception {
		var request = post(ProvisioningRedirectFilter.DEFAULT_PROVISIONING_URL)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("email", "ci@konfigyr.com")
				.param("namespace", "konfigyr")
				.param("type", NamespaceType.TEAM.name())
				.param("firstName", "CI")
				.param("lastName", "Konfigyr")
				.with(csrf());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isBadRequest())
				.andExpect(view().name("accounts/provision"))
				.andExpect(model().attributeErrorCount("form", 1))
				.andExpect(model().attributeHasFieldErrorCode("form", "namespace", "errors.namespace.exists"));
	}

	@Test
	@DisplayName("should render provisioning page with general provisioning error")
	void shouldRenderGeneralProvisioningError() throws Exception {
		var request = post(ProvisioningRedirectFilter.DEFAULT_PROVISIONING_URL)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("email", "ci@konfigyr.com")
				.param("namespace", RandomStringUtils.randomAlphanumeric(300))
				.param("type", NamespaceType.TEAM.name())
				.param("firstName", "CI")
				.param("lastName", "Konfigyr")
				.with(csrf());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isBadRequest())
				.andExpect(view().name("accounts/provision"))
				.andExpect(model().attributeErrorCount("form", 1));
	}
}
