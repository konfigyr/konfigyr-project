package com.konfigyr.artifactory.ownership.controller;

import com.konfigyr.artifactory.Owner;
import com.konfigyr.artifactory.OwnerResolver;
import com.konfigyr.artifactory.ownership.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.CollectionModel;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.support.SearchQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping("/namespaces/{namespace}/group-verifications")
public class GroupVerificationsController {

	private final GroupVerifications groupVerifications;
	private final OwnerResolver ownerResolver;

	@GetMapping
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	public PagedModel<EntityModel<GroupVerification>> getGroupVerifications(
			@PathVariable String namespace,
			@Nullable @RequestParam(required = false) String term,
			@Nullable @RequestParam(required = false) VerificationState state,
			Pageable pageable
	) {
		final Owner owner = ownerResolver.resolve(namespace);

		final SearchQuery query = SearchQuery.builder()
				.pageable(pageable)
				.term(term)
				.criteria(GroupVerification.STATE_CRITERIA, state)
				.build();

		return assembler(namespace).groupVerification().assemble(groupVerifications.findByOwner(owner, query));
	}

	@GetMapping("/{groupId}")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	public EntityModel<GroupVerification> getGroupVerification(@PathVariable String namespace, @PathVariable String groupId) {
		final Owner owner = ownerResolver.resolve(namespace);
		final GroupVerification verification = groupVerifications.findByGroupId(owner, groupId)
				.orElseThrow(() -> new GroupVerificationNotFoundException(owner, groupId));
		return assembler(namespace).groupVerification().assemble(verification);
	}

	@PostMapping
	@PreAuthorize("isAdmin(#namespace)")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	public EntityModel<GroupVerification> claim(@PathVariable String namespace, @RequestBody @Validated ClaimRequest request) {
		final Owner owner = ownerResolver.resolve(namespace);
		final GroupVerification verification = groupVerifications.claim(owner, request.groupId(), request.verificationMethod());

		return assembler(namespace).groupVerification().assemble(verification);
	}

	@GetMapping("/{verificationId}/verification-challenges")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	public CollectionModel<EntityModel<VerificationChallenge>> getVerificationChallenges(@PathVariable String namespace, @PathVariable  EntityId verificationId) {
		final Owner owner = ownerResolver.resolve(namespace);
		final List<VerificationChallenge> verificationChallenges = groupVerifications.findChallenges(owner, verificationId);
		return assembler(namespace).verificationChallenge().assemble(verificationChallenges);
	}


	@PostMapping("/{groupId}/verify")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	public EntityModel<GroupVerification>  verify(
			@PathVariable String namespace,
			@PathVariable String groupId
	) {
		final Owner owner = ownerResolver.resolve(namespace);
		final GroupVerification updated = groupVerifications.verify(owner, groupId);
		return assembler(namespace).groupVerification().assemble(updated);
	}

	@DeleteMapping("/{groupId}")
	@PreAuthorize("isAdmin(#namespace)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	public void revoke(@PathVariable String namespace, @PathVariable String groupId) {
		final Owner owner = ownerResolver.resolve(namespace);
		final GroupVerification verification = groupVerifications.findByGroupId(owner, groupId)
				.orElseThrow(() -> new GroupVerificationNotFoundException(owner, groupId));
		groupVerifications.revoke(verification);
	}

	private GroupVerificationAssembler assembler(String namespace) {
		return new GroupVerificationAssembler(namespace);
	}

	public record ClaimRequest(
			@NotBlank String groupId,
			@NotNull VerificationMethod verificationMethod) {
	}

}
