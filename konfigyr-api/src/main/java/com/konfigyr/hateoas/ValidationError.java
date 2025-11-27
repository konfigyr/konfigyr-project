package com.konfigyr.hateoas;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Record that represents a validation error detail on the {@link org.springframework.http.ProblemDetail} error
 * response.
 *
 * @param detail the details of the validation error, can't be {@literal null}
 * @param pointer pointer to the field that had the validation error, can be {@literal null}
 * @param parameter name of the HTTP request parameter that had the validation error, can be {@literal null}
 * @param header name of the HTTP header that had the validation error, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationError(
		@NonNull String detail,
		@Nullable String pointer,
		@Nullable String parameter,
		@Nullable String header
) {

	public static ValidationError pointer(String name, String detail) {
		return new ValidationError(detail, name, null, null);
	}

	public static ValidationError parameter(String name, String detail) {
		return new ValidationError(detail, null, name, null);
	}

	public static ValidationError header(String name, String detail) {
		return new ValidationError(detail, null, null, name);
	}

}
