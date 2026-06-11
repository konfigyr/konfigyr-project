package com.konfigyr.identity.authorization;

import com.konfigyr.security.NamespaceApplicationSettings;

/**
 * The names for all the {@link org.springframework.security.oauth2.server.authorization.client.RegisteredClient}
 * configuration settings that are specific to a namespace.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public final class NamespaceClientSettingNames {

	private static final String NAMESPACE_PREFIX = "settings.namespace.";

	private NamespaceClientSettingNames() {
	}

	/**
	 * Key under which the {@code Namespace} entity identifier is stored as a custom attribute
	 * on the Spring Security
	 * {@link org.springframework.security.oauth2.server.authorization.settings.ClientSettings} of the
	 * {@link org.springframework.security.oauth2.server.authorization.client.RegisteredClient}.
	 */
	public static final String NAMESPACE = NAMESPACE_PREFIX.concat("id");

	/**
	 * Key under which the {@link com.konfigyr.security.NamespaceClientType} is stored as a
	 * custom attribute on the Spring Security
	 * {@link org.springframework.security.oauth2.server.authorization.settings.ClientSettings} of the
	 * {@link org.springframework.security.oauth2.server.authorization.client.RegisteredClient}.
	 */
	public static final String CLIENT_TYPE = NAMESPACE_PREFIX.concat("type");

	/**
	 * Key under which the {@link NamespaceApplicationSettings.WorkloadSettings#issuerUri()} is stored
	 * as a custom attribute on the Spring Security
	 * {@link org.springframework.security.oauth2.server.authorization.settings.ClientSettings} of the
	 * {@link org.springframework.security.oauth2.server.authorization.client.RegisteredClient}.
	 */
	public static final String WORKLOAD_ISSUER_URI = NAMESPACE_PREFIX.concat("workload.issuer-uri");

	/**
	 * Key under which the {@link NamespaceApplicationSettings.WorkloadSettings#subjectPattern()} is stored
	 * as a custom attribute on the Spring Security
	 * {@link org.springframework.security.oauth2.server.authorization.settings.ClientSettings} of the
	 * {@link org.springframework.security.oauth2.server.authorization.client.RegisteredClient}.
	 */
	public static final String WORKLOAD_SUBJECT_PATTERN = NAMESPACE_PREFIX.concat("workload.subject-pattern");
}
