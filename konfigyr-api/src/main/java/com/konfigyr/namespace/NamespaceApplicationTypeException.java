package com.konfigyr.namespace;

import com.konfigyr.security.NamespaceClientType;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.io.Serial;

/**
 * Exception thrown when an operation is attempted on a {@link NamespaceApplication} whose
 * {@link NamespaceClientType} does not support it.
 * <p>
 * Each {@link ErrorCode} describes a distinct type constraint violation. The code drives
 * both the HTTP status and the message source lookup key, allowing user-facing text to be
 * defined per-violation in the {@code problem-detail.properties} bundle without subclassing.
 * <p>
 * The fallback detail message (used when the message source has no matching key) is set by
 * {@link ErrorCode#format(NamespaceClientType)} using the enum constant's template string.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see NamespaceClientType
 * @see ErrorCode
 */
@NullMarked
public class NamespaceApplicationTypeException extends NamespaceException {

	@Serial
	private final static long serialVersionUID = 5510993132330990231L;

	/** The specific constraint that was violated. */
	private final ErrorCode reason;

	/** The client type of the application on which the unsupported operation was attempted. */
	private final NamespaceClientType type;

	/**
	 * Creates a {@link NamespaceApplicationTypeException} for an attempt to reset the client
	 * secret of an application whose type does not support client secrets.
	 *
	 * @param clientType the type of the application, can't be {@literal null}
	 */
	public NamespaceApplicationTypeException(NamespaceClientType clientType) {
		this(ErrorCode.SECRET_NOT_SUPPORTED, clientType);
	}

	private NamespaceApplicationTypeException(ErrorCode errorCode, NamespaceClientType clientType) {
		super(errorCode.statusCode, errorCode.format(clientType));
		this.reason = errorCode;
		this.type = clientType;
	}

	/**
	 * Returns the {@link ErrorCode} that identifies which type constraint was violated
	 * and determines the HTTP status and message source key used in the response.
	 *
	 * @return error code, never {@literal null}
	 */
	public ErrorCode getErrorCode() {
		return reason;
	}

	/**
	 * Returns the {@link NamespaceClientType} of the application on which the
	 * unsupported operation was attempted.
	 *
	 * @return client type, never {@literal null}
	 */
	public NamespaceClientType getClientType() {
		return type;
	}

	/**
	 * Returns the message source key used to resolve the user-facing detail string.
	 * The key is qualified with the {@link ErrorCode} name so each violation can carry
	 * its own message in the bundle.
	 */
	@Override
	public String getDetailMessageCode() {
		return "problemDetail." + getClass().getName() + "." + reason.name();
	}

	/**
	 * Returns the message source key used to resolve the user-facing title string.
	 * The key is qualified with the {@link ErrorCode} name so each violation can carry
	 * its own title in the bundle.
	 */
	@Override
	public String getTitleMessageCode() {
		return "problemDetail.title." + getClass().getName() + "." + reason.name();
	}

	/**
	 * Returns the arguments interpolated into the message source detail string.
	 * The single argument is the {@link NamespaceClientType#displayName()} of the offending
	 * client type, referenced as {@code {0}} in the bundle (e.g. {@code "AI Agent"}).
	 */
	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[]{ type.displayName() };
	}

	/**
	 * Lists the distinct type constraint violations that can cause a
	 * {@link NamespaceApplicationTypeException} to be thrown.
	 * <p>
	 * Each constant carries the HTTP status code to return and a developer-readable
	 * fallback message template (used when the message source bundle has no matching key).
	 * The template accepts one {@code %s} placeholder for the {@link NamespaceClientType}.
	 */
	@RequiredArgsConstructor
	public enum ErrorCode {

		/**
		 * Thrown when a client secret operation (e.g., secret reset) is attempted on an
		 * application whose {@link NamespaceClientType} does not use a client secret.
		 * <p>
		 * Only {@link NamespaceClientType#AGENT} triggers this, it is a true public client
		 * running on a user's device where a secret cannot be stored securely.
		 * {@link NamespaceClientType#SERVICE_ACCOUNT} and {@link NamespaceClientType#WORKLOAD}
		 * are confidential clients and always carry a secret.
		 */
		SECRET_NOT_SUPPORTED(HttpStatus.UNPROCESSABLE_CONTENT,
				"Cannot reset client secret: %s applications do not use client secrets");

		private final HttpStatusCode statusCode;
		private final String message;

		String format(NamespaceClientType type) {
			return message.formatted(type.name());
		}
	}

}
