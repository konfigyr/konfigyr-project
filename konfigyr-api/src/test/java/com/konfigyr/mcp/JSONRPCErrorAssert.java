package com.konfigyr.mcp;

import io.modelcontextprotocol.spec.McpSchema.JSONRPCResponse.JSONRPCError;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.jspecify.annotations.NonNull;

/**
 * Assert class that should be used to test {@link JSONRPCErrorAssert}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class JSONRPCErrorAssert extends AbstractObjectAssert<JSONRPCErrorAssert, JSONRPCError> {

	/**
	 * Creates a new {@link JSONRPCErrorAssert} with the given problem detail to check.
	 *
	 * @param error the actual JSON RPC error to verify
	 * @return Problem detail assert
	 */
	@NonNull
	public static JSONRPCErrorAssert assertThat(JSONRPCError error) {
		return new JSONRPCErrorAssert(error);
	}

	/**
	 * Create an {@link InstanceOfAssertFactory} that can be used to create {@link JSONRPCErrorAssert} for
	 * an asserted object.
	 *
	 * @return JSON RPC error assert factory
	 */
	@NonNull
	public static InstanceOfAssertFactory<JSONRPCError, JSONRPCErrorAssert> factory() {
		return new InstanceOfAssertFactory<>(JSONRPCError.class, JSONRPCErrorAssert::new);
	}

	JSONRPCErrorAssert(JSONRPCError error) {
		super(error, JSONRPCErrorAssert.class);
	}

	/**
	 * Checks if the given {@link JSONRPCError} has a matching error code.
	 *
	 * @param code error code
	 * @return the JSON RPC error assert instance, never {@literal null}
	 */
	public JSONRPCErrorAssert hasErrorCode(int code) {
		isNotNull();

		Assertions.assertThat(actual.code())
				.as("Expected that JSON RPC error should have an error code of \"%s\" but was \"%s\"", code, actual.code())
				.isEqualTo(code);

		return myself;
	}

	/**
	 * Checks if the given {@link JSONRPCError} has a matching message.
	 *
	 * @param message error message
	 * @param args    the arguments to be used for formatting the sequence
	 * @return the JSON RPC error assert instance, never {@literal null}
	 */
	public JSONRPCErrorAssert hasMessage(String message, Object... args) {
		isNotNull();

		Assertions.assertThat(actual.message())
				.as("Expected that JSON RPC error should have a message \"%s\" but was \"%s\"", message, actual.message())
				.isEqualTo(message, args);

		return myself;
	}

	/**
	 * Checks if the given {@link JSONRPCError} has a matching message.
	 *
	 * @param message error message
	 * @return the JSON RPC error assert instance, never {@literal null}
	 */
	public JSONRPCErrorAssert hasMessageContaining(String message) {
		isNotNull();

		Assertions.assertThat(actual.message())
				.as("Expected that JSON RPC error message should contain: \"%s\"", message)
				.containsSequence(message);

		return myself;
	}

	/**
	 * Checks if the given {@link JSONRPCError} has a matching error data.
	 *
	 * @param data expected error data
	 * @return the JSON RPC error assert instance, never {@literal null}
	 */
	public JSONRPCErrorAssert hasData(Object data) {
		isNotNull();

		Assertions.assertThat(actual.data())
				.as("Expected that JSON RPC error should have a data \"%s\" but was \"%s\"", data, actual.data())
				.isEqualTo(data);

		return myself;
	}

}
