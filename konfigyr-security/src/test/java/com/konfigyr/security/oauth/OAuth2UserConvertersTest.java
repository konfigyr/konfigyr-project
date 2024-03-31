package com.konfigyr.security.oauth;

import com.konfigyr.account.AccountRegistration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.LinkedHashMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

/**
 * @author Vladimir Spasic
 **/
@ExtendWith(MockitoExtension.class)
class OAuth2UserConvertersTest {

	@Mock
	ClientRegistration registration;

	@MethodSource("attributes")
	@DisplayName("should create registration using OAuth attribute converter")
	@ParameterizedTest(name = "should convert oauth user for {0} provider: {1} - {2} - {3}")
	void shouldConvertOAuthUser(String provider, String email, String name, String avatar) {
		doReturn(provider).when(registration).getRegistrationId();

		final var user = createUser(email, name, avatar);

		assertThat(OAuth2UserConverters.get(registration).convert(user))
				.isNotNull()
				.returns(email, AccountRegistration::email)
				.returns(avatar, AccountRegistration::avatar)
				.satisfies(it -> {
					if (StringUtils.isBlank(name)) {
						assertThat(it)
								.returns(null, AccountRegistration::firstName)
								.returns(null, AccountRegistration::lastName);
					} else {
						assertThat(it)
								.returns("John", AccountRegistration::firstName)
								.returns("Doe", AccountRegistration::lastName);
					}
				});
	}

	@Test
	@DisplayName("should fail to find user attribute converter")
	void shouldFailToFindConverter() {
		doReturn("unknown").when(registration).getRegistrationId();

		assertThatThrownBy(() -> OAuth2UserConverters.get(registration))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unsupported OAuth Client registration")
				.hasMessageContaining(registration.getRegistrationId())
				.hasNoCause();
	}

	static Stream<Arguments> attributes() {
		return Stream.of("github", "gitlab").flatMap(provider -> Stream.of(
				Arguments.of(provider, "john.doe@konfigyr.com", null, null),
				Arguments.of(provider, "john.doe@konfigyr.com", "", null),
				Arguments.of(provider, "john.doe@konfigyr.com", "  ", null),
				Arguments.of(provider, "john.doe@konfigyr.com", "John Doe", null),
				Arguments.of(provider, "john.doe@konfigyr.com", "John Doe", "https://example.com/avatar.gif")
		));
	}

	static OAuth2User createUser(String email, String name, String avatar) {
		final var attributes = new LinkedHashMap<String, Object>();
		attributes.put("email", email);
		attributes.put("name", name);
		attributes.put("avatar_url", avatar);

		return new DefaultOAuth2User(
				AuthorityUtils.createAuthorityList("test-scope"),
				attributes,
				"email"
		);
	}

}