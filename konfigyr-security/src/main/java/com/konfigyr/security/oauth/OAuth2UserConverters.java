package com.konfigyr.security.oauth;

import com.konfigyr.account.AccountRegistration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Utility class that holds {@link Converter converters} that would create an {@link AccountRegistration}
 * from the {@link OAuth2User}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
final class OAuth2UserConverters {

	private OAuth2UserConverters() {
	}

	@NonNull
	static Converter<OAuth2User, AccountRegistration> get(@NonNull ClientRegistration registration) {
		return switch (registration.getRegistrationId()) {
			case "github" -> OAuth2UserConverters::github;
			case "gitlab" -> OAuth2UserConverters::gitlab;
			default -> throw new IllegalArgumentException(
					"Unsupported OAuth Client registration: " + registration.getRegistrationId()
			);
		};
	}

	private static AccountRegistration github(@NonNull OAuth2User user) {
		return AccountRegistration.builder()
				.email(user.getName())
				.fullName(user.getAttribute("name"))
				.avatar(user.getAttribute("avatar_url"))
				.build();
	}

	private static AccountRegistration gitlab(@NonNull OAuth2User user) {
		return AccountRegistration.builder()
				.email(user.getName())
				.fullName(user.getAttribute("name"))
				.avatar(user.getAttribute("avatar_url"))
				.build();
	}

}
