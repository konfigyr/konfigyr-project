package com.konfigyr.identity.authorization;

import com.konfigyr.security.NamespaceClientType;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.security.oauth2.server.authorization.autoconfigure.servlet.OAuth2AuthorizationServerProperties;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
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
	 * Audience(s) to include in access tokens issued by this authorization server.
	 * <p>
	 * Resource servers that accept these tokens must validate that the {@code aud} claim contains at least
	 * one of these values before granting access.
	 */
	@NotEmpty
	private Set<String> audiences = new HashSet<>();

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
	private OAuth2AuthorizationServerProperties.Token token = new OAuth2AuthorizationServerProperties.Token();

	/**
	 * Per-type token settings overrides for namespace OAuth applications. Each entry overrides
	 * specific token properties for that client type; fields left unset fall back to the values
	 * defined in the global token settings. Types with no entry inherit all values from the
	 * global token settings without modification.
	 */
	private Map<NamespaceClientType, NamespaceTokenSettings> namespaceTokenSettings = new LinkedHashMap<>();

	/**
	 * Customize how the Authorization Server caches registered OAuth clients.
	 * <p>
	 * It is recommended that entries should be kept in the cache for no longer than 5 minutes,
	 * and therefore we recommend using the following specification: {@code expireAfterWrite=5m}.
	 */
	@NestedConfigurationProperty
	private CacheProperties.Caffeine cache = new CacheProperties.Caffeine();

	/**
	 * Configuration for the trusted issuer registry that resolves and caches the JWK
	 * sources used to verify JWT subject tokens during OAuth 2.0 Token Exchange.
	 */
	@NestedConfigurationProperty
	private TrustedIssuersProperties trustedIssuers = new TrustedIssuersProperties();

	/**
	 * Token settings overrides for a specific namespace client type. Fields left unset fall
	 * back to the global token settings defined in the enclosing authorization properties.
	 * Platform-level constraints such as refresh token rotation policy are always enforced
	 * by the authorization server regardless of what is configured here.
	 */
	@Data
	public static class NamespaceTokenSettings {

		/**
		 * Maximum lifetime of an access token issued to this client type. When not set, the
		 * global token access-token-time-to-live value is used. For Workload Identity
		 * applications this should be kept short to match the expected execution time of a
		 * single CI/CD job.
		 */
		@DurationUnit(ChronoUnit.MINUTES)
		Duration accessTokenTimeToLive;

		/**
		 * Maximum lifetime of a refresh token issued to this client type. When not set, the
		 * global token refresh-token-time-to-live value is used. Has no effect on client types
		 * that do not support refresh tokens, such as Service Account and Workload Identity
		 * applications.
		 */
		@DurationUnit(ChronoUnit.MINUTES)
		Duration refreshTokenTimeToLive;

	}

	/**
	 * Configuration properties for the trusted issuer registry cache. Controls how long
	 * inactive JWK source entries are retained and the maximum number of issuers held
	 * concurrently. Use a Caffeine specification string to configure both constraints,
	 * for example: {@code "maximumSize=1000,expireAfterAccess=7d"}.
	 */
	@Data
	public static class TrustedIssuersProperties {

		/**
		 * Caffeine cache specification for the JWK source cache. Controls the maximum
		 * number of trusted issuers and the inactivity TTL after which an entry is evicted
		 * and its JWK source session torn down. The entry is rebuilt transparently on the
		 * next token exchange request for that issuer.
		 * <p>
		 * It is recommended to set a long inactivity TTL relative to the expected interval
		 * between token exchange requests, for example: {@code "maximumSize=1000,expireAfterAccess=7d"}.
		 */
		@NestedConfigurationProperty
		private CacheProperties.Caffeine cache = new CacheProperties.Caffeine();

	}

}
