package com.konfigyr.artifactory.ownership.controller;

import com.konfigyr.artifactory.ownership.GroupIdAlreadyClaimedException;
import com.konfigyr.artifactory.ownership.GroupVerification;
import com.konfigyr.artifactory.ownership.VerificationChallengeNotFoundException;
import com.konfigyr.artifactory.ownership.GroupVerifications;
import com.konfigyr.artifactory.ownership.Owner;
import com.konfigyr.artifactory.ownership.OwnerNotFoundException;
import com.konfigyr.artifactory.ownership.VerificationChallenge;
import com.konfigyr.artifactory.ownership.VerificationMethod;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.CollectionModel;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping("/namespaces/{namespace}/group-verifications")
public class GroupVerificationsController {

	private final GroupVerifications groupVerifications;

	@GetMapping
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	public CollectionModel<EntityModel<GroupVerification>> getGroupVerifications(@PathVariable String namespace) {
		final Owner owner = resolveOwner(namespace);
		final GroupVerificationAssembler assembler = new GroupVerificationAssembler(namespace);
		final List<GroupVerification> verifications = groupVerifications.findByOwner(owner);
		return assembler.groupVerification().assemble(verifications);
	}

	@GetMapping("/{groupId}")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	public EntityModel<GroupVerification> getGroupVerification(@PathVariable String namespace, @PathVariable String groupId) {
		final Owner owner = resolveOwner(namespace);
		final GroupVerificationAssembler assembler = new GroupVerificationAssembler(namespace);
		final GroupVerification verification = groupVerifications.findByGroupId(groupId, owner)
				.orElseThrow(() -> new VerificationChallengeNotFoundException(owner, groupId));
		return assembler.groupVerification().assemble(verification);
	}

	@PostMapping
	@PreAuthorize("isAdmin(#namespace)")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	public EntityModel<GroupVerification> claim(@PathVariable String namespace, @RequestBody @Validated ClaimRequest request) {
		final Owner owner = resolveOwner(namespace);
		final GroupVerificationAssembler assembler = new GroupVerificationAssembler(namespace);

		groupVerifications.findAnyOverlapping(request.groupId()).ifPresent(ignore -> {
			throw new GroupIdAlreadyClaimedException(request.groupId());
		});

		final GroupVerification verification = groupVerifications.claim(owner, request.groupId(), request.verificationMethod());

		return assembler.groupVerification().assemble(verification);
	}

	@GetMapping("/{verificationId}/verification-challenges")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	public CollectionModel<EntityModel<VerificationChallenge>> getVerificationChallenges(@PathVariable String namespace, @PathVariable  EntityId verificationId) {
		final Owner owner = resolveOwner(namespace);
		final GroupVerificationAssembler assembler = new GroupVerificationAssembler(namespace);
		final List<VerificationChallenge> verificationChallenges = groupVerifications.findChallenges(verificationId, owner);
		return assembler.verificationChallenge().assemble(verificationChallenges);
	}


	@PostMapping("/{groupId}/verify")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	public EntityModel<GroupVerification>  verify(
			@PathVariable String namespace,
			@PathVariable String groupId
	) {
		final Owner owner = resolveOwner(namespace);
		final GroupVerificationAssembler assembler = new GroupVerificationAssembler(namespace);

		final GroupVerification updated = groupVerifications.verify(owner, groupId);
		return assembler.groupVerification().assemble(updated);
	}

	@DeleteMapping("/{groupId}")
	@PreAuthorize("isAdmin(#namespace)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	public void revoke(@PathVariable String namespace, @PathVariable String groupId) {
		final Owner owner = resolveOwner(namespace);
		final GroupVerification verification = groupVerifications.findByGroupId(groupId, owner)
				.orElseThrow(() -> new VerificationChallengeNotFoundException("Could not find a verification for groupId '" + groupId + "' owned by namespace " + owner.slug()));

		groupVerifications.save(verification.revoke());
	}

	private Owner resolveOwner(String slug) {
		return groupVerifications.findOwner(slug)
				.orElseThrow(() -> new OwnerNotFoundException(slug));
	}

	private GroupVerification lookup(Owner owner, String groupId) {
		return groupVerifications.findByGroupId(groupId, owner)
				.orElseThrow(() -> new VerificationChallengeNotFoundException(owner, groupId));
	}

	public record ClaimRequest(
			@NotBlank String groupId,
			@NotNull VerificationMethod verificationMethod) {
	}

}
