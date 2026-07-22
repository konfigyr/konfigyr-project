package com.konfigyr.audit.controller;

import com.konfigyr.audit.AuditEventRepository;
import com.konfigyr.audit.AuditRecord;
import com.konfigyr.data.CursorPageable;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.CursorModel;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.NamespaceNotFoundException;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.support.SearchQuery;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequiresScope(OAuthScope.READ_NAMESPACES)
@RequestMapping("/namespaces/{slug}/audit")
class AuditController {

	private final RepresentationModelAssembler<AuditRecord, EntityModel<AuditRecord>> assembler = EntityModel::of;

	private final NamespaceManager namespaces;
	private final AuditEventRepository repository;

	@GetMapping
	@PreAuthorize("isMember(#slug)")
	CursorModel<EntityModel<AuditRecord>> find(
			@PathVariable String slug,
			@Nullable @RequestParam(required = false) String eventType,
			@Nullable @RequestParam(required = false) String entityType,
			@Nullable @RequestParam(required = false) EntityId entityId,
			@Nullable @RequestParam(required = false) String actor,
			@Nullable @RequestParam(required = false) OffsetDateTime from,
			@Nullable @RequestParam(required = false) OffsetDateTime to,
			CursorPageable pageable
	) {
		final Namespace namespace = namespaces.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
		final SearchQuery query = SearchQuery.builder()
				.criteria(AuditRecord.NAMESPACE_ID_CRITERIA, namespace.id())
				.criteria(AuditRecord.EVENT_TYPE_CRITERIA, eventType)
				.criteria(AuditRecord.ENTITY_TYPE_CRITERIA, entityType)
				.criteria(AuditRecord.ENTITY_ID_CRITERIA, entityId)
				.criteria(AuditRecord.ACTOR_ID_CRITERIA, actor)
				.criteria(AuditRecord.FROM_CRITERIA, from)
				.criteria(AuditRecord.TO_CRITERIA, to)
				.pageable(Pageable.unpaged())
				.build();

		return assembler.assemble(repository.find(query, pageable));
	}

}
