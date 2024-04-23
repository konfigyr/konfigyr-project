package com.konfigyr.account.settings;

import com.konfigyr.NamespaceTestConfiguration;
import com.konfigyr.account.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.test.TestAccounts;
import com.konfigyr.test.TestPrincipals;
import jakarta.servlet.ServletException;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest(classes = NamespaceTestConfiguration.class)
class AccountSettingsTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	AccountManager manager;

	@Test
	@DisplayName("should generate model for account profile settings page that can not be deleted")
	void shouldRenderProfilePage() throws Exception {
		final var request = get("/account")
				.with(authentication(TestPrincipals.john()));

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isOk())
				.andExpect(model().attribute("id", EntityId.from(1)))
				.andExpect(model().attribute("form", matches(value -> assertThat(value)
						.isNotNull()
						.isInstanceOf(AccountSettingsForm.class)
						.asInstanceOf(InstanceOfAssertFactories.type(AccountSettingsForm.class))
						.returns("john.doe@konfigyr.com", AccountSettingsForm::getEmail)
						.returns("John", AccountSettingsForm::getFirstName)
						.returns("Doe", AccountSettingsForm::getLastName)
				)))
				.andExpect(model().attribute("account", matches(value -> assertThat(value)
						.isNotNull()
						.isInstanceOf(Account.class)
						.asInstanceOf(InstanceOfAssertFactories.type(Account.class))
						.returns(EntityId.from(1), Account::id)
						.returns(false, Account::isDeletable)
				)))
				.andExpect(model().attribute("memberships", matches(value -> assertThat(value)
						.isNotNull()
						.isInstanceOf(Memberships.class)
						.asInstanceOf(InstanceOfAssertFactories.iterable(Membership.class))
						.hasSize(2)
				)))
				.andExpect(model().attribute("namespaceNames", "konfigyr"))
				.andExpect(view().name("accounts/profile"));
	}

	@Test
	@DisplayName("should generate model for account profile settings page that can be deleted")
	void shouldRenderProfilePageForDeletableUser() throws Exception {
		final var request = get("/account")
				.with(authentication(TestPrincipals.jane()));

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isOk())
				.andExpect(model().attribute("id", EntityId.from(2)))
				.andExpect(model().attribute("form", matches(value -> assertThat(value)
						.isNotNull()
						.isInstanceOf(AccountSettingsForm.class)
						.asInstanceOf(InstanceOfAssertFactories.type(AccountSettingsForm.class))
						.returns("jane.doe@konfigyr.com", AccountSettingsForm::getEmail)
						.returns("Jane", AccountSettingsForm::getFirstName)
						.returns("Doe", AccountSettingsForm::getLastName)
				)))
				.andExpect(model().attribute("account", matches(value -> assertThat(value)
						.isNotNull()
						.isInstanceOf(Account.class)
						.asInstanceOf(InstanceOfAssertFactories.type(Account.class))
						.returns(EntityId.from(2), Account::id)
						.returns(true, Account::isDeletable)
				)))
				.andExpect(model().attribute("memberships", matches(value -> assertThat(value)
						.isNotNull()
						.isInstanceOf(Memberships.class)
						.asInstanceOf(InstanceOfAssertFactories.iterable(Membership.class))
						.hasSize(1)
				)))
				.andExpect(model().attribute("namespaceNames", "konfigyr"))
				.andExpect(view().name("accounts/profile"));
	}

	@Test
	@DisplayName("should fail to render account profile settings page for unknown user")
	void shouldNotRenderProfilePageForUnknown() {
		final var account = TestAccounts.john()
				.id(381623L)
				.build();

		final var request = get("/account")
				.with(authentication(TestPrincipals.from(account)));

		assertThatThrownBy(() -> mvc.perform(request))
				.isInstanceOf(ServletException.class)
				.hasRootCauseInstanceOf(AccountNotFoundException.class);
	}

	@Test
	@Transactional
	@DisplayName("should submit account settings form and update account")
	void shouldSubmitSettingsForm() throws Exception {
		final var request = multipart(HttpMethod.POST, "/account")
				.param("email", "irulan.corrino@empire.com")
				.param("firstName", "Irulan")
				.param("lastName", "Corrino")
				.with(authentication(TestPrincipals.jane()))
				.with(csrf());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/account"));

		assertThat(manager.findById(EntityId.from(2)))
				.isNotEmpty()
				.get()
				.returns("jane.doe@konfigyr.com", Account::email)
				.returns("Irulan", Account::firstName)
				.returns("Corrino", Account::lastName);
	}

	@Test
	@DisplayName("should fail to update account when form is invalid")
	void shouldValidateSettingsForm() throws Exception {
		final var request = multipart(HttpMethod.POST, "/account")
				.param("email", "jane.doe@konfigyr.com")
				.with(authentication(TestPrincipals.jane()))
				.with(csrf());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isBadRequest())
				.andExpect(model().errorCount(2))
				.andExpect(model().attributeHasFieldErrorCode("form", "firstName", "NotEmpty"))
				.andExpect(model().attributeHasFieldErrorCode("form", "lastName", "NotEmpty"))
				.andExpect(model().attribute("form", matches(value -> assertThat(value)
						.isNotNull()
						.isInstanceOf(AccountSettingsForm.class)
						.asInstanceOf(InstanceOfAssertFactories.type(AccountSettingsForm.class))
						.returns("jane.doe@konfigyr.com", AccountSettingsForm::getEmail)
						.returns(null, AccountSettingsForm::getFirstName)
						.returns(null, AccountSettingsForm::getLastName)
				)))
				.andExpect(view().name("accounts/profile"));

		assertThat(manager.findById(EntityId.from(2)))
				.isPresent()
				.get()
				.returns("Jane", Account::firstName)
				.returns("Doe", Account::lastName);
	}

	@Test
	@DisplayName("should fail to update account when it does not exist")
	void shouldFailToUpdateAccountThatDoesNotExist() {
		final var account = TestAccounts.john()
				.id(381623L)
				.build();

		final var request = multipart(HttpMethod.POST, "/account")
				.param("email", account.email())
				.param("firstName", account.firstName())
				.param("lastName", account.lastName())
				.with(authentication(TestPrincipals.from(account)))
				.with(csrf());

		assertThatThrownBy(() -> mvc.perform(request).andDo(log()))
				.isInstanceOf(ServletException.class)
				.hasRootCauseInstanceOf(AccountNotFoundException.class);
	}

	@Test
	@DisplayName("should fail to update account when CSRF Token is not sent")
	void shouldFailToUpdateAccountDueToMissingCsrfToken() throws Exception {
		final var request = multipart(HttpMethod.POST, "/account")
				.with(authentication(TestPrincipals.jane()));

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isForbidden());

		assertThat(manager.findById(EntityId.from(2)))
				.isPresent()
				.get()
				.returns("Jane", Account::firstName)
				.returns("Doe", Account::lastName);
	}

	@Test
	@Transactional
	@DisplayName("should submit account delete form and delete account")
	void shouldSubmitDeleteForm() throws Exception {
		final var request = multipart(HttpMethod.POST, "/account/delete")
				.with(authentication(TestPrincipals.jane()))
				.with(csrf());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"));

		assertThat(manager.findById(EntityId.from(2))).isEmpty();
	}

	@Test
	@DisplayName("should fail to delete account when he is an admin member")
	void shouldFailToDeleteAccount() throws Exception {
		final var request = multipart(HttpMethod.POST, "/account/delete")
				.with(authentication(TestPrincipals.john()))
				.with(csrf());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isBadRequest())
				.andExpect(model().errorCount(1))
				.andExpect(model().attributeExists("form"))
				.andExpect(model().attributeExists("account"))
				.andExpect(model().attributeExists("memberships"))
				.andExpect(model().attributeExists("namespaceNames"));

		assertThat(manager.findById(EntityId.from(1))).isPresent();
	}

	@Test
	@DisplayName("should fail to delete account when it does not exist")
	void shouldFailToDeleteAccountThatDoesNotExist() {
		final var account = TestAccounts.john()
				.id(381623L)
				.build();

		final var request = multipart(HttpMethod.POST, "/account/delete")
				.with(authentication(TestPrincipals.from(account)))
				.with(csrf());

		assertThatThrownBy(() -> mvc.perform(request).andDo(log()))
				.isInstanceOf(ServletException.class)
				.hasRootCauseInstanceOf(AccountNotFoundException.class);
	}

	@Test
	@DisplayName("should fail to delete account when CSRF Token is not sent")
	void shouldFailToDeleteAccountDueToMissingCsrfToken() throws Exception {
		final var request = multipart(HttpMethod.POST, "/account/delete")
				.with(authentication(TestPrincipals.jane()));

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().isForbidden());

		assertThat(manager.findById(EntityId.from(2))).isPresent();
	}

	static <T> Matcher<T> matches(Consumer<T> matcher) {
		return new BaseMatcher<>() {
			private AssertionError failure;

			@Override
			@SuppressWarnings("unchecked")
			public boolean matches(Object o) {
				failure = null;

				try {
					matcher.accept((T) o);
				} catch (AssertionError ex) {
					failure = ex;
				}

				return failure == null;
			}

			@Override
			public void describeTo(Description description) {
				if (failure != null) {
					description.appendText(failure.getMessage());
				}
			}
		};
	}

}