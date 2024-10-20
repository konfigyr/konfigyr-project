package com.konfigyr.integration;

import org.springframework.lang.NonNull;

import java.util.Set;

/**
 * Enumeration that defines supported third-party integration providers and how they can be used within the
 * Konfigyr ecosystem.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see IntegrationProvider
 */
public enum IntegrationProvider {

	/**
	 * GitHub integration provider that supports following integrations.
	 * <ul>
	 *     <li>{@link IntegrationType#OAUTH OAuth authentication}</li>
	 *     <li>{@link IntegrationType#SOURCE_CODE Source code integration}</li>
	 * </ul>
	 */
	GITHUB("GitHub", IntegrationType.OAUTH, IntegrationType.SOURCE_CODE);

	private final String label;
	private final String icon;
	private final Set<IntegrationType> types;

	IntegrationProvider(String label, IntegrationType... types) {
		this(label, label.toLowerCase(), types);
	}

	IntegrationProvider(String label, String icon, IntegrationType... types) {
		this.label = label;
		this.icon = icon;
		this.types = Set.of(types);
	}

	/**
	 * The display label for this {@link IntegrationProvider} that is shown the UI. Usually the official
	 * or name of the third-party provider.
	 *
	 * @return the integration provider label, never {@literal null}
	 */
	@NonNull
	public String getLabel() {
		return label;
	}

	/**
	 * The icon name that should be used by the {@link IntegrationProvider}.
	 *
	 * @return the integration provider icon name, never {@literal null}
	 * @see <a href="https://feathericons.com">Feather Icons</a>
	 */
	@NonNull
	public String getIcon() {
		return icon;
	}

	/**
	 * Returns a set of supported {@link IntegrationType integration types} for the given {@link IntegrationProvider}.
	 *
	 * @return supported integration types for the provider, never {@literal null}.
	 */
	@NonNull
	public Set<IntegrationType> getSupportedTypes() {
		return types;
	}
}
