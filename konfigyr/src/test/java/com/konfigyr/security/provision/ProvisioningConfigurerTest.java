package com.konfigyr.security.provision;

import com.konfigyr.test.config.SpringTestContext;
import com.konfigyr.test.config.SpringTestContextExtension;
import com.konfigyr.security.PrincipalService;
import com.konfigyr.test.TestPrincipals;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith({
		MockitoExtension.class,
		SpringTestContextExtension.class
})
class ProvisioningConfigurerTest {
	SpringTestContext context = SpringTestContext.create()
			.postProcessor(ProvisioningConfigurerTest::setupMockMvc);

	@Mock
	static RequestCache requestCache;

	@Mock
	static RememberMeServices rememberMeServices;

	@Mock
	static PrincipalService principalService;

	@Autowired
	MockMvc mvc;

	@Test
	@DisplayName("should redirect to provisioning page and provision account with default configuration")
	void shouldProvisionUsingDefaults() throws Exception {
		context.register(DefaultProvisioningConfigurer.class).autowire();

		final var principal = TestPrincipals.john().getPrincipal();
		doReturn(principal).when(principalService).lookup("test-account");

		var request = get("/some-page")
				.sessionAttr(WebAttributes.AUTHENTICATION_EXCEPTION, new ProvisioningRequiredException());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(ProvisioningRedirectFilter.DEFAULT_PROVISIONING_URL));

		request = get(ProvisioningAuthenticationFilter.DEFAULT_PROCESSING_URL)
				.queryParam("account", "test-account")
				.with(dispatcherType(DispatcherType.FORWARD));

		mvc.perform(request)
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"))
				.andExpect(authenticated().withAuthenticationPrincipal(principal));

		verify(rememberMeServices).loginSuccess(any(), any(), any());
		verify(principalService).lookup("test-account");
	}

	@ValueSource(strings = {
			"/ignored/pattern",
			"/ignored/matcher",
			"/account/provision"
	})
	@ParameterizedTest(name = "should not redirect for request URI: {0}")
	@DisplayName("should not redirect to provisioning page for ignored request patterns")
	void shouldNotRedirectForIgnoredRequests(String url) throws Exception {
		context.register(CustomProvisioningConfigurer.class).autowire();

		final var request = get(url)
				.sessionAttr(WebAttributes.AUTHENTICATION_EXCEPTION, new ProvisioningRequiredException());

		mvc.perform(request)
				.andDo(log())
				// for these pages there are no request mappings, so 404 is expected
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("should redirect to provisioning page and provision account with custom configuration")
	void shouldProvisionUsingCustomSettings() throws Exception {
		context.register(CustomProvisioningConfigurer.class).autowire();

		final var principal = TestPrincipals.john().getPrincipal();
		doReturn(principal).when(principalService).lookup("test-account");

		var request = get("/some-page")
				.sessionAttr(WebAttributes.AUTHENTICATION_EXCEPTION, new ProvisioningRequiredException());

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/account/provision"));

		request = get("/account/authenticate")
				.queryParam("account", "test-account")
				.with(dispatcherType(DispatcherType.FORWARD));

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"))
				.andExpect(authenticated().withAuthenticationPrincipal(principal));

		verify(rememberMeServices).loginSuccess(any(), any(), any());
		verify(principalService).lookup("test-account");
	}

	@Test
	@DisplayName("should redirect after provisioning to a saved request")
	void shouldProvisionAccountAndRedirectToCachedRequest() throws Exception {
		context.register(RequestCacheConfig.class, DefaultProvisioningConfigurer.class).autowire();

		final var principal = TestPrincipals.john().getPrincipal();
		doReturn(principal).when(principalService).lookup("test-account");

		final var request = get(ProvisioningAuthenticationFilter.DEFAULT_PROCESSING_URL)
				.queryParam("account", "test-account")
				.with(dispatcherType(DispatcherType.FORWARD));

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"))
				.andExpect(authenticated().withAuthenticationPrincipal(principal));

		verify(requestCache).getRequest(any(), any());
	}

	@Test
	@SuppressWarnings("unchecked")
	@DisplayName("should post process provisioning security filters")
	void shouldPostProcessFilters() {
		context.register(PostProcessingProvisioningConfigurer.class).autowire();

		final ObjectPostProcessor<Object> processor = context.get().getBean(ObjectPostProcessor.class);

		verify(processor).postProcess(any(ProvisioningRedirectFilter.class));
		verify(processor).postProcess(any(ProvisioningAuthenticationFilter.class));
	}

	@Test
	@DisplayName("should redirect to an error page when account parameter is specified")
	void shouldRedirectToErrorPageWhenAccountParameterIsNotPresent() throws Exception {
		context.register(CustomProvisioningConfigurer.class).autowire();

		final var request = get("/account/authenticate")
				.with(dispatcherType(DispatcherType.FORWARD));

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/account/failed"))
				.andExpect(unauthenticated());

		verifyNoInteractions(principalService);
	}

	@Test
	@DisplayName("should redirect to an error page after failed authentication")
	void shouldRedirectToErrorPageWhenPrincipalIsNotFound() throws Exception {
		context.register(CustomProvisioningConfigurer.class).autowire();

		doThrow(UsernameNotFoundException.class).when(principalService).lookup("test-account");

		final var request = get("/account/authenticate")
				.queryParam("account", "test-account")
				.with(dispatcherType(DispatcherType.FORWARD));

		mvc.perform(request)
				.andDo(log())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/account/failed"))
				.andExpect(unauthenticated());
	}

	static void setupMockMvc(ConfigurableWebApplicationContext context) {
		final MockMvc mvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();

		context.getBeanFactory().registerResolvableDependency(MockMvc.class, mvc);
	}

	static RequestPostProcessor dispatcherType(DispatcherType type) {
		return request -> {
			request.setDispatcherType(type);
			return request;
		};
	}

	@EnableWebSecurity
	static class DefaultProvisioningConfigurer {
		@Bean
		SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
			return http
					.requestCache(Customizer.withDefaults())
					.rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices))
					.with(new ProvisioningConfigurer<>(), provision -> provision
							.principalService(principalService)
					)
					.build();
		}
	}

	@EnableWebSecurity
	static class CustomProvisioningConfigurer {
		@Bean
		SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
			return http
					.requestCache(Customizer.withDefaults())
					.rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices))
					.with(new ProvisioningConfigurer<>(), provision -> provision
							.provisioningRedirectUrl("/account/provision")
							.provisioningProcessingUrl("/account/authenticate")
							.ignoringRequestMatchers("/ignored/pattern")
							.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/ignored/matcher"))
							.redirectStrategy(new DefaultRedirectStrategy())
							.failureHandler(new SimpleUrlAuthenticationFailureHandler("/account/failed"))
							.principalService(principalService)
					)
					.build();
		}
	}

	@EnableWebSecurity
	static class PostProcessingProvisioningConfigurer {

		@Bean
		static ObjectPostProcessor<Object> objectPostProcessor() {
			return spy(new NoopProcessor());
		}

		@Bean
		SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
			return http
					.with(new ProvisioningConfigurer<>(), provision -> provision
							.principalService(principalService)
					)
					.build();
		}

		static class NoopProcessor implements ObjectPostProcessor<Object> {
			@Override
			public <O> O postProcess(O object) {
				return object;
			}
		}
	}

	static class RequestCacheConfig {

		@Bean
		RequestCache testRequestCache() {
			return requestCache;
		}

	}
}
