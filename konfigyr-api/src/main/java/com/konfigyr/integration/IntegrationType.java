package com.konfigyr.integration;

import org.springframework.lang.NonNull;

/**
 * Enumeration that lists supported third-party integration and defines it's use case and nature.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see IntegrationProvider
 */
public enum IntegrationType {

	/**
	 * Integration type that allows this service to use {@code OAuth} to authorize users.
	 */
	OAUTH("key"),

	/**
	 * Used by {@link IntegrationProvider integration providers} from which the repositories can
	 * be imported in Konfigyr service.
	 */
	SOURCE_CODE("code");

	private final String icon;

	IntegrationType(String icon) {
		this.icon = icon;
	}

	/**
	 * The icon name that should be used by the {@link IntegrationType}.
	 *
	 * @return the integration type icon name, never {@literal null}
	 * @see <a href="https://feathericons.com">Feather Icons</a>
	 */
	@NonNull
	public String getIcon() {
		return icon;
	}
}
