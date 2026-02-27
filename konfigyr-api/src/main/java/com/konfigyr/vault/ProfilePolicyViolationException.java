package com.konfigyr.vault;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation violates the policy specified in the {@link Profile}.
 * <p>
 * This exception indicates that the requested state mutation is not allowed due to
 * the {@link ProfilePolicy} assigned to the profile.
 * <p>
 * As this violation represents a business rule violation, it should be translated to an
 * HTTP 409 (Conflict) response.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class ProfilePolicyViolationException extends VaultException {

	private final ViolationReason reason;
	private final Profile profile;

	/**
	 * Creates a new {@link ProfilePolicyViolationException} for the given profile with the
	 * {@link ViolationReason#IMMUTABLE_PROFILE} reason.
	 *
	 * @param profile the profile that was the subject of the policy violation, cannot be {@literal null}.
	 * @return the profile policy violation exception, never {@literal null}.
	 */
	public static ProfilePolicyViolationException immutableProfile(Profile profile) {
		return new ProfilePolicyViolationException(ViolationReason.IMMUTABLE_PROFILE, profile);
	}

	/**
	 * Creates a new {@link ProfilePolicyViolationException} for the given profile with the
	 * {@link ViolationReason#PROTECTED_PROFILE} reason.
	 *
	 * @param profile the profile that was the subject of the policy violation, cannot be {@literal null}.
	 * @return the profile policy violation exception, never {@literal null}.
	 */
	public static ProfilePolicyViolationException protectedProfile(Profile profile) {
		return new ProfilePolicyViolationException(ViolationReason.PROTECTED_PROFILE, profile);
	}

	/**
	 * Creates a new {@link ProfilePolicyViolationException} with the specified reason and profile.
	 *
	 * @param reason the reason for the profile policy violation, cannot be {@literal null}.
	 * @param profile the profile that was the subject of the policy violation, cannot be {@literal null}.
	 */
	public ProfilePolicyViolationException(ViolationReason reason, Profile profile) {
		super(HttpStatus.CONFLICT, reason.format(profile));
		this.reason = reason;
		this.profile = profile;
		getBody().setProperty("reason", reason.name());
	}

	/**
	 * Returns the reason for the profile policy violation.
	 *
	 * @return the violation reason, never {@literal null}.
	 */
	@NonNull
	public ViolationReason getReason() {
		return reason;
	}

	/**
	 * Returns the profile that was the subject of the policy violation.
	 *
	 * @return the profile, never {@literal null}.
	 */
	@NonNull
	public Profile getProfile() {
		return profile;
	}

	/**
	 * Returns the profile policy that was violated.
	 *
	 * @return the violated profile policy, never {@literal null}.
	 */
	@NonNull
	public ProfilePolicy getPolicy() {
		return profile.policy();
	}

	@NonNull
	@Override
	public String getDetailMessageCode() {
		return "problemDetail." + getClass().getName() + "." + reason.name();
	}

	@NonNull
	@Override
	public String getTitleMessageCode() {
		return "problemDetail.title." + getClass().getName() + "." + reason.name();
	}

	@Override
	public Object @Nullable [] getDetailMessageArguments() {
		return new Object[] { profile.name() };
	}

	/**
	 * Enumeration used to provide the reason for the profile policy violation.
	 */
	@RequiredArgsConstructor
	public enum ViolationReason {
		/**
		 * Any state change was attempted on an immutable, read-only profile.
		 */
		IMMUTABLE_PROFILE("Profile '%s' is read-only and does not allow state modifications."),

		/**
		 * Direct state change was attempted on a protected profile.
		 */
		PROTECTED_PROFILE("Profile '%s' is protected. Changes must be submitted for approval.");

		private final String message;

		private String format(Profile profile) {
			return message.formatted(profile.slug());
		}
	}

}
