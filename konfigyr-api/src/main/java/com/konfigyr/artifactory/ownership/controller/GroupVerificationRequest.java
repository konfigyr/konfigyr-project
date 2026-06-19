package com.konfigyr.artifactory.ownership.controller;

import com.konfigyr.artifactory.ownership.VerificationMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GroupVerificationRequest(
		@NotBlank String groupId,
		@NotNull VerificationMethod verificationMethod) {
}
