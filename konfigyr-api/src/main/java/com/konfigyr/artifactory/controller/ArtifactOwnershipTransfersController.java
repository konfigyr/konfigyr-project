package com.konfigyr.artifactory.controller;

import com.konfigyr.artifactory.Owner;
import com.konfigyr.artifactory.OwnerResolver;
import com.konfigyr.artifactory.transfer.ArtifactOwnershipTransfer;
import com.konfigyr.artifactory.transfer.ArtifactOwnershipTransferAlreadyResolvedException;
import com.konfigyr.artifactory.transfer.ArtifactOwnershipTransferNotFoundException;
import com.konfigyr.artifactory.transfer.ArtifactOwnershipTransfers;
import com.konfigyr.artifactory.transfer.TransferState;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.support.SearchQuery;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping("/namespaces/{namespace}/artifact-transfers")
class ArtifactOwnershipTransfersController {

	private final ArtifactOwnershipTransfers transfers;
	private final OwnerResolver ownerResolver;

	@GetMapping
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	PagedModel<EntityModel<ArtifactOwnershipTransfer>> getArtifactOwnershipTransfers(
			@PathVariable String namespace,
			@RequestParam Direction direction,
			@Nullable @RequestParam(required = false) String term,
			Pageable pageable
	) {
		final Owner owner = ownerResolver.resolve(namespace);

		final SearchQuery query = SearchQuery.builder()
				.pageable(pageable)
				.term(term)
				.build();

		final var page = direction == Direction.INCOMING
				? transfers.findIncoming(owner, query)
				: transfers.findOutgoing(owner, query);

		return Assemblers.artifactOwnershipTransfer(owner).assemble(page);
	}

	@GetMapping("/{id}")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	EntityModel<ArtifactOwnershipTransfer> getArtifactOwnershipTransfer(@PathVariable String namespace, @PathVariable EntityId id) {
		final Owner owner = ownerResolver.resolve(namespace);
		final ArtifactOwnershipTransfer transfer = findTransfer(owner, id);

		return Assemblers.artifactOwnershipTransfer(owner).assemble(transfer);
	}

	@PostMapping
	@PreAuthorize("isAdmin(#namespace)")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<ArtifactOwnershipTransfer> request(@PathVariable String namespace, @RequestBody @Validated TransferRequest request) {
		final Owner to = ownerResolver.resolve(namespace);
		final Owner from = ownerResolver.resolve(request.fromNamespace());

		return Assemblers.artifactOwnershipTransfer(to)
				.assemble(transfers.request(to, request.groupId(), from));
	}

	@PostMapping("/{id}/accept")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<ArtifactOwnershipTransfer> accept(@PathVariable String namespace, @PathVariable EntityId id) {
		final Owner owner = ownerResolver.resolve(namespace);
		final ArtifactOwnershipTransfer transfer = findTransfer(owner, id);

		if (!owner.equals(transfer.from())) {
			throw new AccessDeniedException("Only the '%s' namespace may accept this transfer".formatted(transfer.from().slug()));
		}

		return Assemblers.artifactOwnershipTransfer(owner).assemble(transfers.accept(requirePending(transfer)));
	}

	@PostMapping("/{id}/reject")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<ArtifactOwnershipTransfer> reject(@PathVariable String namespace, @PathVariable EntityId id) {
		final Owner owner = ownerResolver.resolve(namespace);
		final ArtifactOwnershipTransfer transfer = findTransfer(owner, id);

		if (!owner.equals(transfer.from())) {
			throw new AccessDeniedException("Only the '%s' namespace may reject this transfer".formatted(transfer.from().slug()));
		}

		return Assemblers.artifactOwnershipTransfer(owner).assemble(transfers.reject(requirePending(transfer)));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<ArtifactOwnershipTransfer> cancel(@PathVariable String namespace, @PathVariable EntityId id) {
		final Owner owner = ownerResolver.resolve(namespace);
		final ArtifactOwnershipTransfer transfer = findTransfer(owner, id);

		if (!owner.equals(transfer.to())) {
			throw new AccessDeniedException("Only the '%s' namespace may cancel this transfer".formatted(transfer.to().slug()));
		}

		return Assemblers.artifactOwnershipTransfer(owner).assemble(transfers.cancel(requirePending(transfer)));
	}

	private ArtifactOwnershipTransfer findTransfer(Owner owner, EntityId id) {
		return transfers.findById(owner, id)
				.orElseThrow(() -> new ArtifactOwnershipTransferNotFoundException(owner, id));
	}

	private static ArtifactOwnershipTransfer requirePending(ArtifactOwnershipTransfer transfer) {
		if (transfer.state() != TransferState.PENDING) {
			throw new ArtifactOwnershipTransferAlreadyResolvedException(transfer);
		}

		return transfer;
	}

	enum Direction {
		INCOMING,
		OUTGOING
	}

	record TransferRequest(@NotBlank String groupId, @NotBlank String fromNamespace) { /* noop */ }

}
