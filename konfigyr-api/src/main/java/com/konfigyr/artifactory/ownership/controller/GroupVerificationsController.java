package com.konfigyr.artifactory.ownership.controller;

import com.konfigyr.artifactory.ownership.GroupIdAlreadyClaimedException;
import com.konfigyr.artifactory.ownership.GroupVerification;
import com.konfigyr.artifactory.ownership.GroupVerificationException;
import com.konfigyr.artifactory.ownership.GroupVerificationNotFoundException;
import com.konfigyr.artifactory.ownership.GroupVerifications;
import com.konfigyr.artifactory.ownership.Owner;
import com.konfigyr.artifactory.ownership.OwnerNotFoundException;
import com.konfigyr.artifactory.ownership.VerificationChallenge;
import com.konfigyr.artifactory.ownership.VerificationMethod;
import com.konfigyr.artifactory.ownership.VerificationResult;
import com.konfigyr.artifactory.ownership.VerificationStrategy;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.CollectionModel;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
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
@RequiresScope(OAuthScope.WRITE_NAMESPACES)
@RequestMapping("/namespaces/{namespace}/group-verifications")
public class GroupVerificationsController {

	private final GroupVerifications groupVerifications;
	private final VerificationStrategy dnsTxtVerificationStrategy;

	@GetMapping
	@PreAuthorize("isAdmin(#namespace)")
	public CollectionModel<EntityModel<GroupVerification>> getGroupVerifications(@PathVariable String namespace) {
		final Owner owner = resolveOwner(namespace);
		final GroupVerificationAssembler assembler = new GroupVerificationAssembler(namespace);
		final List<GroupVerification> verifications = groupVerifications.findByOwner(owner);
		return assembler.groupVerification().assemble(verifications);
	}

	@GetMapping("/{groupId}")
	@PreAuthorize("isAdmin(#namespace)")
	public EntityModel<GroupVerification> getGroupVerification(@PathVariable String namespace, @PathVariable String groupId) {
		final Owner owner = resolveOwner(namespace);
		final GroupVerificationAssembler assembler = new GroupVerificationAssembler(namespace);
		final GroupVerification verification = groupVerifications.findByGroupId(groupId, owner)
				.orElseThrow(() -> new GroupVerificationNotFoundException(owner, groupId));
		return assembler.groupVerification().assemble(verification);
	}

	@PostMapping
	@PreAuthorize("isAdmin(#namespace)")
	@ResponseStatus(HttpStatus.CREATED)
	public EntityModel<GroupVerification> claim(@PathVariable String namespace, @RequestBody @Validated GroupVerificationRequest request) {
		final Owner owner = resolveOwner(namespace);
		final GroupVerificationAssembler assembler = new GroupVerificationAssembler(namespace);

		groupVerifications.findAnyOverlapping(request.groupId()).ifPresent(ignore -> {
			throw new GroupIdAlreadyClaimedException(request.groupId());
		});

		final GroupVerification verification = groupVerifications.save(
				GroupVerification.claim(owner, request.groupId())
		);

		VerificationChallenge verificationChallenge = VerificationChallenge.issue(request.verificationMethod())
				.toBuilder()
				.verificationId(verification.id())
				.build();
		groupVerifications.saveChallenge(verificationChallenge);

		return assembler.groupVerification().assemble(verification);
	}

	@GetMapping("/{verificationId}/verification-challenges")
	@PreAuthorize("isAdmin(#namespace)")
	public CollectionModel<EntityModel<VerificationChallenge>> getVerificationChallenges(@PathVariable String namespace, @PathVariable  EntityId verificationId) {
		final Owner owner = resolveOwner(namespace);
		final GroupVerificationAssembler assembler = new GroupVerificationAssembler(namespace);
		final List<VerificationChallenge> verificationChallenges = groupVerifications.findChallenges(verificationId, owner);
		return assembler.verificationChallenge().assemble(verificationChallenges);
	}


	@PostMapping("/{groupId}/verify")
	@PreAuthorize("isAdmin(#namespace)")
	public EntityModel<GroupVerification>  verify(
			@PathVariable String namespace,
			@PathVariable String groupId
	) {
		final Owner owner = resolveOwner(namespace);
		final GroupVerificationAssembler assembler = new GroupVerificationAssembler(namespace);
		final GroupVerification verification = lookup(owner, groupId);
		final VerificationChallenge challenge = groupVerifications.findActiveChallenge(verification)
				.orElseThrow(() -> new GroupVerificationException("No active challenge to verify for groupId " + groupId));

		final VerificationStrategy strategy = resolveStrategy(challenge.method());

		final VerificationResult result = strategy.verify(verification, challenge);
		groupVerifications.saveChallenge(challenge.applyResult(result));

		final GroupVerification updated = result instanceof VerificationResult.Success ? groupVerifications.save(verification.activate()) : verification;

		return assembler.groupVerification().assemble(updated);
	}

	@DeleteMapping("/{groupId}")
	@PreAuthorize("isAdmin(#namespace)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void revoke(@PathVariable String namespace, @PathVariable String groupId) {
		final Owner owner = resolveOwner(namespace);
		final GroupVerification verification = groupVerifications.findByGroupId(groupId, owner)
				.orElseThrow(() -> new GroupVerificationNotFoundException(owner, groupId));

		groupVerifications.save(verification.revoke());
	}

	private Owner resolveOwner(String slug) {
		return groupVerifications.findOwner(slug)
				.orElseThrow(() -> new OwnerNotFoundException(slug));
	}

	private VerificationStrategy resolveStrategy(VerificationMethod model) {
		return switch (model) {
			case DNS -> dnsTxtVerificationStrategy;
			case GITHUB -> dnsTxtVerificationStrategy;
		};
	}

	private GroupVerification lookup(Owner owner, String groupId) {
		return groupVerifications.findByGroupId(groupId, owner)
				.orElseThrow(() -> new GroupVerificationNotFoundException(owner, groupId));
	}

}
