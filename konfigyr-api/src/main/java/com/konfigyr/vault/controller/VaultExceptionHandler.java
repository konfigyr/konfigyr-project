package com.konfigyr.vault.controller;

import com.konfigyr.vault.state.RepositoryStateException;
import com.konfigyr.vault.state.RepositoryStateException.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
class VaultExceptionHandler {

	private final MessageSource messageSource;

	@NonNull
	@ExceptionHandler(RepositoryStateException.class)
	ResponseEntity<@NonNull ProblemDetail> handleRepositoryStateException(
			@NonNull WebRequest request,
			@NonNull RepositoryStateException ex
	) {
		final ErrorResponse response = switch (ex.getErrorCode()) {
			case UNAVAILABLE -> unavailable(request, ex);
			case INVALID_STATE -> invalidState(request, ex);
			case CORRUPTED_STATE -> corruptedState(request, ex);
			case CONFLICT -> conflict(request, ex);
			case INITIALIZATION_FAILED -> initializationFailed(request, ex);
			case UNKNOWN_REPOSITORY -> unknownRepository(request, ex);
			case REPOSITORY_ALREADY_EXISTS -> repositoryAlreadyExists(request, ex);
			case UNKNOWN_PROFILE -> unknownProfile(request, ex);
			case PROFILE_ALREADY_EXISTS -> profileAlreadyExists(request, ex);
			case UNKNOWN_CHANGESET -> unknownChangeset(request, ex);
			default -> unknown(request, ex);
		};

		final ProblemDetail body = response.updateAndGetBody(messageSource, LocaleContextHolder.getLocale());
		return new ResponseEntity<>(body, response.getHeaders(), response.getStatusCode());
	}

	/**
	 * Handles the {@link ErrorCode#UNAVAILABLE} error code that means that the repository cannot be accessed
	 * either due to timeout or IO failure. This handler should perform the following:
	 * <ul>
	 *     <li>Trigger a monitoring alert as this is infrastructure degradation</li>
	 *     <li>Should not update the state as it may be a temporary issue</li>
	 *     <li>Hide technical details from the user</li>
	 * </ul>
	 *
	 * @param request the current request, never {@literal null}
	 * @param ex the repository state exception, never {@literal null}
	 * @return the error response to be shown to the user, never {@literal null}
	 */
	private ErrorResponse unavailable(@NonNull WebRequest request, @NonNull RepositoryStateException ex) {
		return createErrorResponseFor(request, ex, HttpStatus.SERVICE_UNAVAILABLE).build();
	}

	/**
	 * Handles the {@link ErrorCode#INVALID_STATE} error code that means that the repository is not in a
	 * consistent state and may be corrupted. This handler should perform the following:
	 * <ul>
	 *     <li>Trigger a monitoring alert as this is operational corruption</li>
	 *     <li>Should update the state as it is in an unhealthy state</li>
	 *     <li>Hide technical details from the user as they can't repair the repo</li>
	 * </ul>
	 *
	 * @param request the current request, never {@literal null}
	 * @param ex the repository state exception, never {@literal null}
	 * @return the error response to be shown to the user, never {@literal null}
	 */
	private ErrorResponse invalidState(@NonNull WebRequest request, @NonNull RepositoryStateException ex) {
		return createErrorResponseFor(request, ex, HttpStatus.SERVICE_UNAVAILABLE).build();
	}

	/**
	 * Handles the {@link ErrorCode#CORRUPTED_STATE} error code that means that the repository can't encode or
	 * decode the configuration state as it may be corrupted. This handler should perform the following:
	 * <ul>
	 *     <li>Trigger a monitoring alert as this is a data integrity violation</li>
	 *     <li>Should update the state as it's state is in a corrupted state</li>
	 *     <li>Hide technical details from the user as they can't repair the repo</li>
	 * </ul>
	 *
	 * @param request the current request, never {@literal null}
	 * @param ex the repository state exception, never {@literal null}
	 * @return the error response to be shown to the user, never {@literal null}
	 */
	private ErrorResponse corruptedState(@NonNull WebRequest request, @NonNull RepositoryStateException ex) {
		return createErrorResponseFor(request, ex, HttpStatus.INTERNAL_SERVER_ERROR).build();
	}

	/**
	 * Handles the {@link ErrorCode#CONFLICT} error code that means that the repository encountered a
	 * structural or concurrency failure (should not be mistaken with Git conflict). Example would be if
	 * the git lock was lost or a rebase was required. This handler should perform the following:
	 * <ul>
	 *     <li>Should not trigger a monitoring alert as this is operational concurrency</li>
	 *     <li>Should not update the state as it is a transient state</li>
	 *     <li>Inform the user about the failure but omit the Git internals if possible</li>
	 * </ul>
	 *
	 * @param request the current request, never {@literal null}
	 * @param ex the repository state exception, never {@literal null}
	 * @return the error response to be shown to the user, never {@literal null}
	 */
	private ErrorResponse conflict(@NonNull WebRequest request, @NonNull RepositoryStateException ex) {
		return createErrorResponseFor(request, ex, HttpStatus.CONFLICT).build();
	}

	/**
	 * Handles the {@link ErrorCode#INITIALIZATION_FAILED} error code that means that the repository can't
	 * be created. This handler should perform the following:
	 * <ul>
	 *     <li>Trigger a monitoring alert as this is a provisioning failure</li>
	 *     <li>Should update the state as it can't be provisioned</li>
	 *     <li>Hide technical details from the user as they may contain FS errors</li>
	 * </ul>
	 *
	 * @param request the current request, never {@literal null}
	 * @param ex the repository state exception, never {@literal null}
	 * @return the error response to be shown to the user, never {@literal null}
	 */
	private ErrorResponse initializationFailed(@NonNull WebRequest request, @NonNull RepositoryStateException ex) {
		return createErrorResponseFor(request, ex, HttpStatus.INTERNAL_SERVER_ERROR).build();
	}

	/**
	 * Handles the {@link ErrorCode#UNKNOWN_REPOSITORY} error code that means that the repository can't
	 * be located for the service as it is removed from the FS. This handler should perform the following:
	 * <ul>
	 *     <li>Not trigger a monitoring alert as this is likely a logical error</li>
	 *     <li>Should not update the state as it is a transient error</li>
	 *     <li>Nothing to hide from the user</li>
	 * </ul>
	 *
	 * @param request the current request, never {@literal null}
	 * @param ex the repository state exception, never {@literal null}
	 * @return the error response to be shown to the user, never {@literal null}
	 */
	private ErrorResponse unknownRepository(@NonNull WebRequest request, @NonNull RepositoryStateException ex) {
		return createErrorResponseFor(request, ex, HttpStatus.NOT_FOUND).build();
	}

	/**
	 * Handles the {@link ErrorCode#REPOSITORY_ALREADY_EXISTS} error code that means that the repository is
	 * already created for the service. This handler should perform the following:
	 * <ul>
	 *     <li>Not trigger a monitoring alert as this was most likely a misuse or a race condition</li>
	 *     <li>Should not update the state as it is a transient error</li>
	 *     <li>Nothing to hide from the user</li>
	 * </ul>
	 *
	 * @param request the current request, never {@literal null}
	 * @param ex the repository state exception, never {@literal null}
	 * @return the error response to be shown to the user, never {@literal null}
	 */
	private ErrorResponse repositoryAlreadyExists(@NonNull WebRequest request, @NonNull RepositoryStateException ex) {
		return createErrorResponseFor(request, ex, HttpStatus.CONFLICT).build();
	}

	/**
	 * Handles the {@link ErrorCode#UNKNOWN_PROFILE} error code that means that the repository can't
	 * locate the profile-specific branch. This handler should perform the following:
	 * <ul>
	 *     <li>Trigger a monitoring alert if this was unexpected, services should catch this error</li>
	 *     <li>Should update the state to invalid as the profile branch is gone</li>
	 *     <li>Nothing to hide from the user</li>
	 * </ul>
	 *
	 * @param request the current request, never {@literal null}
	 * @param ex the repository state exception, never {@literal null}
	 * @return the error response to be shown to the user, never {@literal null}
	 */
	private ErrorResponse unknownProfile(@NonNull WebRequest request, @NonNull RepositoryStateException ex) {
		return createErrorResponseFor(request, ex, HttpStatus.NOT_FOUND).build();
	}

	/**
	 * Handles the {@link ErrorCode#PROFILE_ALREADY_EXISTS} error code that means that the repository
	 * already has a branch for the profile. This handler should perform the following:
	 * <ul>
	 *     <li>Not trigger a monitoring alert as this was most likely a misuse or a race condition</li>
	 *     <li>Should not update the state as it is a transient error</li>
	 *     <li>Nothing to hide from the user</li>
	 * </ul>
	 *
	 * @param request the current request, never {@literal null}
	 * @param ex the repository state exception, never {@literal null}
	 * @return the error response to be shown to the user, never {@literal null}
	 */
	private ErrorResponse profileAlreadyExists(@NonNull WebRequest request, @NonNull RepositoryStateException ex) {
		return createErrorResponseFor(request, ex, HttpStatus.CONFLICT).build();
	}

	/**
	 * Handles the {@link ErrorCode#UNKNOWN_CHANGESET} error code that means that the repository can't
	 * locate the changeset-specific branch. This handler should perform the following:
	 * <ul>
	 *     <li>Trigger a monitoring alert if this was unexpected, services should catch this error</li>
	 *     <li>Should update the state to invalid as the changeset branch is gone</li>
	 *     <li>Nothing to hide from the user</li>
	 * </ul>
	 *
	 * @param request the current request, never {@literal null}
	 * @param ex the repository state exception, never {@literal null}
	 * @return the error response to be shown to the user, never {@literal null}
	 */
	private ErrorResponse unknownChangeset(@NonNull WebRequest request, @NonNull RepositoryStateException ex) {
		return createErrorResponseFor(request, ex, HttpStatus.NOT_FOUND).build();
	}

	/**
	 * Handles the {@link ErrorCode#UNKNOWN} error code that means that we can't exactly pinpoint the cause
	 * of the error. This handler should perform the following:
	 * <ul>
	 *     <li>Trigger a monitoring alert as we could not classify it</li>
	 *     <li>Should not update the state as cause classification is missing</li>
	 *     <li>Never expose anything to the user</li>
	 * </ul>
	 *
	 * @param request the current request, never {@literal null}
	 * @param ex the repository state exception, never {@literal null}
	 * @return the error response to be shown to the user, never {@literal null}
	 */
	private ErrorResponse unknown(@NonNull WebRequest request, @NonNull RepositoryStateException ex) {
		if (log.isWarnEnabled()) {
			log.warn("Caught an unknown repository state exception", ex);
		}

		return createErrorResponseFor(request, ex, HttpStatus.INTERNAL_SERVER_ERROR).build();
	}

	static ErrorResponse.Builder createErrorResponseFor(
			@NonNull WebRequest request,
			@NonNull RepositoryStateException ex,
			@NonNull HttpStatusCode statusCode
	) {
		return createErrorResponseFor(request, ex, ex.getErrorCode().name(), statusCode);
	}

	static ErrorResponse.Builder createErrorResponseFor(
			@NonNull WebRequest request,
			@NonNull RepositoryStateException ex,
			@NonNull String errorCode,
			@NonNull HttpStatusCode statusCode
	) {
		final String messageCodeSuffix = ex.getClass().getName() + "." + errorCode;

		final ErrorResponse.Builder builder = ErrorResponse.builder(ex, statusCode, ex.getMessage())
				.typeMessageCode("problemDetail.type." + messageCodeSuffix)
				.titleMessageCode("problemDetail.title." + messageCodeSuffix)
				.detailMessageCode("problemDetail." + messageCodeSuffix);

		if (request instanceof ServletWebRequest servletWebRequest) {
			final UriComponents instance = ServletUriComponentsBuilder.fromRequestUri(servletWebRequest.getRequest())
					.build();

			builder.instance(instance.toUri());
		}

		return builder;
	}

}
