package com.konfigyr.vault.environment;

import org.jspecify.annotations.NullMarked;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Simple serializable representation of a named source of key-value pairs.
 * <p>
 * This is a simplified DTO equivalent to Spring Cloud Config's {@code PropertySource}.
 *
 * @author Mila Zarkovic
 * @since 1.0.0
 */
@NullMarked
public record PropertySource(String name, Map<?, ?> source) implements Serializable {

	@Serial
	private static final long serialVersionUID = -40708989912941201L;

	@Override
	public String toString() {
		return String.format("PropertySource(name=%s)", this.name);
	}
}
