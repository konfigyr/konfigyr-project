package com.konfigyr.identity.authorization;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration properties type used to configure the behavior of Spring Security OAuth2 Authorization Server.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Getter
@Setter
@ConfigurationProperties("konfigyr.identity.authorization")
public class AuthorizationProperties {

	/**
	 * Client ID of the built-in Konfigyr OAuth client.
	 */
	@NotEmpty
	private String clientId;

	/**
	 * Client secret of the built-in Konfigyr OAuth client. May not be left blank.
	 */
	@NotEmpty
	private String clientSecret;

	/**
	 * Name of the built-in Konfigyr OAuth client.
	 */
	@NotEmpty
	private String clientName = "Konfigyr OAuth Client";

	/**
	 * Redirect URI(s) that the built-in Konfigyr OAuth client may use to redirect the user-agent when using
	 * the {@code authorization_code} grant type.
	 */
	@NotEmpty
	private Set<String> redirectUris = new HashSet<>();

	/**
	 * Redirect URI(s) that the built-in Konfigyr OAuth client may use for logout.
	 */
	private Set<String> postLogoutRedirectUris = new HashSet<>();

	/**
	 * Customize how OAuth 2.0 Authorization Server issues various OAuth Tokens.
	 * <p>
	 * This configuration is used by both Namespace OAuth Applications and the built-in Konfigyr OAuth client.
	 */
	@NestedConfigurationProperty
	OAuth2AuthorizationServerProperties.Token token = new OAuth2AuthorizationServerProperties.Token();

}
