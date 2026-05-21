package com.konfigyr.membership.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.membership.Member;
import com.konfigyr.membership.MemberNotFoundException;
import com.konfigyr.membership.Memberships;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.NamespaceNotFoundException;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.support.SearchQuery;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequiresScope(OAuthScope.INVITE_MEMBERS)
@RequestMapping("/namespaces/{slug}/members")
class MembersController {

	private final NamespaceManager namespaces;
	private final Memberships members;

	@GetMapping
	@PreAuthorize("isMember(#slug)")
	PagedModel<EntityModel<Member>> find(@PathVariable @NonNull String slug, @NonNull Pageable pageable) {
		final SearchQuery query = SearchQuery.of(pageable);
		final Namespace namespace = lookupNamespace(slug);

		return Assemblers.member(namespace)
				.assemble(members.find(namespace, query));
	}

	@GetMapping("/{member}")
	@PreAuthorize("isMember(#slug)")
	EntityModel<Member> get(@PathVariable @NonNull String slug, @PathVariable @NonNull EntityId member) {
		final Namespace namespace = lookupNamespace(slug);

		return Assemblers.member(namespace).assemble(lookupMember(namespace, member));
	}

	@PutMapping("/{member}")
	@PreAuthorize("isAdmin(#slug)")
	EntityModel<Member> update(
			@PathVariable @NonNull String slug,
			@PathVariable @NonNull EntityId member,
			@RequestBody @Validated MemberAttributes attributes
	) {
		final Namespace namespace = lookupNamespace(slug);

		return Assemblers.member(namespace).assemble(members.update(
				namespace, lookupMember(namespace, member).id(), attributes.role()
		));
	}

	@DeleteMapping("/{member}")
	@PreAuthorize("isAdmin(#slug)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void remove(@PathVariable @NonNull String slug, @PathVariable @NonNull EntityId member) {
		final Namespace namespace = lookupNamespace(slug);

		members.remove(namespace, lookupMember(namespace, member).id());
	}

	@NonNull
	Namespace lookupNamespace(String slug) {
		return namespaces.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
	}

	@NonNull
	Member lookupMember(Namespace namespace, EntityId id) {
		return members.get(namespace, id)
				.filter(member -> member.isMemberOf(namespace))
				.orElseThrow(() -> new MemberNotFoundException(id));
	}

	record MemberAttributes(@NotNull NamespaceRole role) {

	}

}
