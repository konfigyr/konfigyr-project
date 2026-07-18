package com.konfigyr.artifactory.controller;

import com.konfigyr.artifactory.Owner;
import com.konfigyr.artifactory.OwnerResolver;
import com.konfigyr.artifactory.ownership.*;
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

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping("/namespaces/{namespace}/group-verifications")
class GroupVerificationsController {

	private final GroupVerifications groupVerifications;
	private final OwnerResolver ownerResolver;

	@GetMapping
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	PagedModel<EntityModel<GroupVerification>> getGroupVerifications(
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

		return Assemblers.groupVerification(owner)
				.assemble(groupVerifications.findByOwner(owner, query));
	}

	@GetMapping("/{groupId}")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	EntityModel<Assemblers.GroupVerificationRepresentation> getGroupVerification(@PathVariable String namespace, @PathVariable String groupId) {
		final Owner owner = ownerResolver.resolve(namespace);
		final GroupVerification verification = groupVerifications.findByGroupId(owner, groupId)
				.orElseThrow(() -> new GroupVerificationNotFoundException(owner, groupId));

		return Assemblers.groupVerification(owner, groupVerifications.findOwners(verification.groupId(), owner))
				.assemble(verification);
	}

	@PostMapping
	@PreAuthorize("isAdmin(#namespace)")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<Assemblers.GroupVerificationRepresentation> claim(@PathVariable String namespace, @RequestBody @Validated ClaimRequest request) {
		final Owner owner = ownerResolver.resolve(namespace);
		final GroupVerification verification = groupVerifications.claim(owner, request.groupId(), request.verificationMethod());

		return Assemblers.groupVerification(owner, groupVerifications.findOwners(request.groupId(), owner))
				.assemble(verification);
	}

	@GetMapping("/{groupId}/challenges")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	CollectionModel<EntityModel<VerificationChallenge>> getVerificationChallenges(@PathVariable String namespace, @PathVariable String groupId) {
		final Owner owner = ownerResolver.resolve(namespace);
		final GroupVerification verification = groupVerifications.findByGroupId(owner, groupId)
				.orElseThrow(() -> new GroupVerificationNotFoundException(owner, groupId));

		return Assemblers.verificationChallenge(owner, verification)
				.assemble(groupVerifications.findChallenges(verification));
	}

	@PostMapping("/{groupId}/verify")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<Assemblers.GroupVerificationRepresentation> verify(
			@PathVariable String namespace,
			@PathVariable String groupId
	) {
		final Owner owner = ownerResolver.resolve(namespace);
		final GroupVerification verification = groupVerifications.verify(owner, groupId);

		return Assemblers.groupVerification(owner, groupVerifications.findOwners(groupId, owner))
				.assemble(verification);
	}

	@DeleteMapping("/{groupId}")
	@PreAuthorize("isAdmin(#namespace)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	void revoke(@PathVariable String namespace, @PathVariable String groupId) {
		final Owner owner = ownerResolver.resolve(namespace);
		final GroupVerification verification = groupVerifications.findByGroupId(owner, groupId)
				.orElseThrow(() -> new GroupVerificationNotFoundException(owner, groupId));

		groupVerifications.revoke(verification);
	}

	record ClaimRequest(@NotBlank String groupId, @NotNull VerificationMethod verificationMethod) { /* noop */ }

}
