package com.konfigyr.namespace.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import com.konfigyr.namespace.*;
import com.konfigyr.support.SearchQuery;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/namespaces/{slug}/members")
class MembersController {

	private final NamespaceManager namespaces;
	private final RepresentationModelAssembler<Member, EntityModel<Member>> assembler = Assemblers.member();

	@GetMapping
	@PreAuthorize("isMember(#slug)")
	PagedModel<EntityModel<Member>> find(@PathVariable @NonNull String slug, @NonNull Pageable pageable) {
		final SearchQuery query = SearchQuery.of(pageable);

		return assembler.assemble(namespaces.findMembers(lookupNamespace(slug), query));
	}

	@GetMapping("/{member}")
	@PreAuthorize("isMember(#slug)")
	EntityModel<Member> get(@PathVariable @NonNull String slug, @PathVariable @NonNull EntityId member) {
		return assembler.assemble(lookupMember(slug, member));
	}

	@PutMapping("/{member}")
	@PreAuthorize("isAdmin(#slug)")
	EntityModel<Member> update(
			@PathVariable @NonNull String slug,
			@PathVariable @NonNull EntityId member,
			@RequestBody @Validated MemberAttributes attributes
	) {
		return assembler.assemble(namespaces.updateMember(
				lookupMember(slug, member).id(), attributes.role()
		));
	}

	@DeleteMapping("/{member}")
	@PreAuthorize("isAdmin(#slug)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void remove(@PathVariable @NonNull String slug, @PathVariable @NonNull EntityId member) {
		namespaces.removeMember(lookupMember(slug, member).id());
	}

	@NonNull
	Namespace lookupNamespace(String slug) {
		return namespaces.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
	}

	@NonNull
	Member lookupMember(String slug, EntityId id) {
		final Namespace namespace = lookupNamespace(slug);

		return namespaces.getMember(id)
				.filter(member -> member.isMemberOf(namespace))
				.orElseThrow(() -> new MemberNotFoundException(id));
	}

	record MemberAttributes(@NotNull NamespaceRole role) {

	}

}
