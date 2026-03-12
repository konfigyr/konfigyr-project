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
 * @author Mila Zarkovic
 * @since 1.0.0
 */
@NullMarked
public record ConfigEnvironment(String name, String[] profiles, List<PropertySource> propertySources) implements Serializable {

	@Serial
	private static final long serialVersionUID = 551214156160016450L;
}
