package com.konfigyr.security;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Type-specific configuration settings for a namespace OAuth2 application. Each {@link NamespaceClientType},
 * except {@link NamespaceClientType#SERVICE_ACCOUNT}, has a corresponding implementation that carries
 * the configuration required to register and operate the OAuth2 client.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see NamespaceClientType
 */
@ValueObject
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = NamespaceApplicationSettings.AgentSettings.class, name = "agent"),
		@JsonSubTypes.Type(value = NamespaceApplicationSettings.WorkloadSettings.class, name = "workload")
})
public sealed interface NamespaceApplicationSettings
		permits NamespaceApplicationSettings.AgentSettings,
				NamespaceApplicationSettings.WorkloadSettings {

	/**
	 * Settings for {@link NamespaceClientType#AGENT} applications.
	 *
	 * @param redirectUris the allowed redirect URIs for the Authorization Code + PKCE flow.
	 *                     Must contain at least one URI. The authorization server rejects any
	 *                     {@code redirect_uri} at token time that is not in this list. Typical
	 *                     values are loopback addresses such as {@code http://localhost/callback}
	 *                     for CLI agents running on a developer's machine.
	 */
	record AgentSettings(@NonNull @NotEmpty List<@NotBlank @URL String> redirectUris) implements NamespaceApplicationSettings {

		public AgentSettings {
			Assert.notEmpty(redirectUris, "AgentSettings requires at least one redirect URI");
			redirectUris = List.copyOf(redirectUris);
		}

	}

	/**
	 * Settings for {@link NamespaceClientType#WORKLOAD} applications.
	 *
	 * @param issuerUri      the OIDC issuer URI of the external identity provider (e.g. GitLab,
	 *                       GitHub Actions, Buildkite) whose tokens are accepted in the RFC 8693
	 *                       token exchange flow. The authorization server fetches
	 *                       {@code <issuerUri>/.well-known/openid-configuration} to validate
	 *                       incoming subject tokens. Must not be {@literal null} or blank.
	 * @param subjectPattern an optional glob or regex pattern applied to the {@code sub} claim of
	 *                       the incoming subject token before the exchange is approved. Use this to
	 *                       restrict access to a specific project, repository, or branch
	 *                       (e.g. {@code "repo:acme/api:ref:refs/heads/main"} for GitHub Actions).
	 *                       When {@literal null}, any {@code sub} value from the trusted issuer is accepted.
	 */
	record WorkloadSettings(@NonNull @NotBlank @URL String issuerUri, @Nullable String subjectPattern)
			implements NamespaceApplicationSettings {

		public WorkloadSettings {
			Assert.hasText(issuerUri, "WorkloadSettings requires a non-blank issuer URI");
		}

	}

}
