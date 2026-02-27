package com.konfigyr.vault.state;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * Exception type thrown by the {@link StateRepository} implementation. They should be thrown when an
 * operation against the underlying source control system (e.g., Git) cannot be completed.
 * <p>
 * This exception represents infrastructure-level failures and must be translated by higher layers into
 * domain-specific errors that can be returned to the client.
 * <p>
 * The {@link RepositoryStateException} uses the {@link ErrorCode} enumeration to specify the exact cause
 * of the problem that was encountered during the failed operation. Implementations of {@link StateRepository}
 * should try to do their best to provide as detailed an error message as possible using the relevant
 * error code.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class RepositoryStateException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = -5579152178839019368L;

	private final ErrorCode errorCode;

	/**
	 * Create a new instance of {@link RepositoryStateException} with the specified error code and message.
	 *
	 * @param errorCode the error code
	 * @param message the error message
	 */
	public RepositoryStateException(@NonNull ErrorCode errorCode, @NonNull String message) {
		super(message);
		this.errorCode = errorCode;
	}

	/**
	 * Create a new instance of {@link RepositoryStateException} with the specified error code, message, and
	 * the exception that caused it.
	 *
	 * @param errorCode the error code
	 * @param message the error message
	 * @param cause the exception cause, or {@literal null} if none
	 */
	public RepositoryStateException(@NonNull ErrorCode errorCode, @NonNull String message, @Nullable Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	/**
	 * The error code that can be used to identify the exact cause of the exception.
	 *
	 * @return the error code, never {@literal null}.
	 */
	@NonNull
	public ErrorCode getErrorCode() {
		return errorCode;
	}

	/**
	 * Enumeration that can be used to specify the exact cause of the exception that occurred
	 * while interacting with the {@link StateRepository}.
	 */
	@RequiredArgsConstructor
	public enum ErrorCode {

		/**
		 * Error code that is used when the {@link StateRepository} is not available to interact with
		 * the underlying source control system or there was a timeout while performing an operation.
		 */
		UNAVAILABLE,

		/**
		 * Error code that is used when the underlying source control system behind the implementation
		 * of the {@link StateRepository} is in an invalid state. This could mean that the repository is
		 * corrupted, the index is inconsistent, etc.
		 */
		INVALID_STATE,

		/**
		 * Error code that is used when the configiration state that is stored in the {@link StateRepository}
		 * can not be encoded or decoded. This could means that the repository is in a corrupted state and
		 * should be repaired.
		 */
		CORRUPTED_STATE,

		/**
		 * Error code that is used when the {@link StateRepository} failed to perform an operation due to
		 * concurrent or structural repository state. It should not be mistaken for a Git conflict.
		 */
		CONFLICT,

		/**
		 * Error code that is used when the {@link StateRepository} failed to initialize the repository
		 * for a {@link com.konfigyr.namespace.Service}.
		 */
		INITIALIZATION_FAILED,

		/**
		 * Error code that is used when the {@link StateRepository} can not resolve a repository the given
		 * {@link com.konfigyr.namespace.Service}.
		 */
		UNKNOWN_REPOSITORY,

		/**
		 * Error code that is used when attempting to create a repository for a {@link com.konfigyr.namespace.Service}
		 * that is already initialized.
		 */
		REPOSITORY_ALREADY_EXISTS,

		/**
		 * Error code that is used when the {@link StateRepository} can not resolve a profile specific branch
		 * for the given {@link com.konfigyr.namespace.Service}.
		 */
		UNKNOWN_PROFILE,

		/**
		 * Error code used when a profile branch is already created for {@link com.konfigyr.namespace.Service}.
		 */
		PROFILE_ALREADY_EXISTS,

		/**
		 * Error code that is used when the {@link StateRepository} can not resolve a changeset branch
		 * for the given {@link com.konfigyr.namespace.Service}.
		 */
		UNKNOWN_CHANGESET,

		/**
		 * Code used to describe either a generic failure or an unknown error. The exact cause of the failure
		 * can be determined by inspecting the exception message and the stack trace.
		 */
		UNKNOWN

	}

}
