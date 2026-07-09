package com.konfigyr.namespace.manifest;

import com.konfigyr.namespace.NamespaceException;
import org.springframework.http.HttpStatusCode;

/**
 * Exception type that would usually be thrown when interacting with {@link ServiceManifests}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class ServiceManifestException extends NamespaceException {

	/**
	 * Create a new instance of {@link ServiceManifestException} with the specified error message.
	 *
	 * @param message the error message
	 */
	public ServiceManifestException(String message) {
		super(message);
	}

	/**
	 * Create a new instance of {@link ServiceManifestException} with the specified error message and HTTP status code.
	 *
	 * @param statusCode HTTP status code
	 * @param message the error message
	 */
	public ServiceManifestException(HttpStatusCode statusCode, String message) {
		super(statusCode, message);
	}

	/**
	 * Create a new instance of {@link ServiceManifestException} with the specified error message and
	 * the exception that caused it.
	 *
	 * @param message the error message
	 * @param cause the exception cause, or {@literal null} if none
	 */
	public ServiceManifestException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Create a new instance of {@link ServiceManifestException} with the specified error message, HTTP status code
	 * and the exception that caused it.
	 *
	 * @param statusCode HTTP status code
	 * @param message the error message
	 * @param cause the exception cause, or {@literal null} if none
	 */
	public ServiceManifestException(HttpStatusCode statusCode, String message, Throwable cause) {
		super(statusCode, message, cause);
	}

}
