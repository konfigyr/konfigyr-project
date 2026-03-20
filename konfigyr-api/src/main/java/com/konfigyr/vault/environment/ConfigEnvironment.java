package com.konfigyr.vault.environment;


import org.jspecify.annotations.NullMarked;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Serializable representation of configuration property sources returned to a configuration client.
 * <p>
 * This is a simplified DTO equivalent to Spring Cloud Config's {@code Environment}.
 *
 * @param name the name of the service (application) for which configuration properties are resolved
 * @param profiles the list of profiles that were requested and used to resolve the configuration (e.g. {@code dev}, {@code prod})
 * @param propertySources the list of property sources for the requested service and profiles
 * @author Mila Zarkovic
 * @since 1.0.0
 */
@NullMarked
public record ConfigEnvironment(String name, String[] profiles, List<PropertySource> propertySources) implements Serializable {

	@Serial
	private static final long serialVersionUID = 551214156160016450L;
}
