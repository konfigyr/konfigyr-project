package com.konfigyr.mcp;

import com.konfigyr.mcp.annotation.McpComponent;
import com.konfigyr.mcp.annotation.McpTool;
import com.konfigyr.mcp.annotation.McpToolParam;
import com.konfigyr.mcp.tool.StructuredCollectionOutput;
import com.konfigyr.mcp.tool.StructuredEntityOutput;
import com.konfigyr.mcp.tool.StructuredOutput;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.Services;
import com.konfigyr.security.KonfigyrClaimNames;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.vault.*;
import com.konfigyr.vault.changes.ChangeRequestManager;
import io.modelcontextprotocol.common.McpTransportContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

/**
 * The {@link McpComponent} bean exposing the namespace's Vault and service MCP tools:
 * {@code list_services}, {@code list_profiles}, {@code list_change_requests}, and
 * {@code get_change_request}.
 * <p>
 * Each tool reads the namespace off the current {@link McpTransportContext}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@McpComponent
@RequiredArgsConstructor
class McpTools {

	private final Services services;
	private final ProfileManager profiles;
	private final ChangeRequestManager changeRequests;

	@McpTool(
			name = "list_services",
			title = "List services",
			description = "List services in the caller's namespace, optionally filtered by name via `term`. Call " +
					"this first whenever a request names a service informally (e.g. 'the payment service') to " +
					"resolve it to the exact slug required by every other tool or resource that takes a `service` " +
					"parameter. Returns each match's slug, name, and description."
	)
	StructuredCollectionOutput<Service> listServices(
			McpTransportContext context,
			@McpToolParam(description = "Search term", required = false) String term
	) {
		final Namespace namespace = (Namespace) context.get(KonfigyrClaimNames.NAMESPACE);
		final SearchQuery query = SearchQuery.builder()
				.term(term)
				.pageable(Pageable.ofSize(10))
				.build();

		return StructuredOutput.of(services.find(namespace, query));
	}

	@McpTool(
			name = "list_profiles",
			title = "List service profiles",
			description = "List `{service}`'s profiles with their slug, name, and policy (`UNPROTECTED`, " +
					"`PROTECTED`, or `IMMUTABLE`), optionally filtered by name via `term`. Use before " +
					"`propose_profile_change` to confirm a profile slug exists and isn't `IMMUTABLE` - proposing " +
					"changes against an `IMMUTABLE` profile will be rejected."
	)
	StructuredCollectionOutput<Profile> listProfiles(
			McpTransportContext context,
			@McpToolParam(name = "service", description = "Unique service slug") String slug,
			@McpToolParam(description = "Profile search term", required = false) String term
	) {
		final Namespace namespace = (Namespace) context.get(KonfigyrClaimNames.NAMESPACE);
		final Service service = services.get(namespace, slug).orElseThrow();

		final SearchQuery query = SearchQuery.builder()
				.term(term)
				.pageable(Pageable.ofSize(10))
				.build();

		return StructuredOutput.of(profiles.find(service, query));
	}

	@McpTool(
			name = "list_change_requests",
			title = "List change requests",
			description = "List change requests for `{service}`, optionally filtered by `{profile}`, " +
					"`{state}` (defaults to `OPEN`), and a text `{query}` matched against subject/description. " +
					"Use before `propose_profile_change` to check for an existing open proposal covering the same " +
					"property before creating a duplicate."
	)
	StructuredCollectionOutput<ChangeRequest> listChangeRequests(
			McpTransportContext context,
			@McpToolParam(name = "service", description = "Unique service slug") String slug,
			@McpToolParam(description = "The profile the change requests belong to", required = false) String profile,
			@McpToolParam(description = "The change request state filter", required = false) ChangeRequestState state,
			@McpToolParam(description = "Change request search term", required = false) String term
	) {
		final Namespace namespace = (Namespace) context.get(KonfigyrClaimNames.NAMESPACE);
		final Service service = services.get(namespace, slug).orElseThrow();

		final SearchQuery query = SearchQuery.builder()
				.term(term)
				.criteria(ChangeRequest.PROFILE_CRITERIA, profile)
				.criteria(ChangeRequest.STATE_CRITERIA, state == null ? ChangeRequestState.OPEN : state)
				.pageable(Pageable.ofSize(10))
				.build();

		return StructuredOutput.of(changeRequests.search(service, query));
	}

	@McpTool(
			name = "get_change_request",
			title = "Retrieve change request",
			description = "One change request's full picture: metadata (`state`, `mergeStatus`, subject, description, " +
					"`createdBy`/`createdAt`), its property diff (`added`/`updated`/`removed`, each with from/to values), " +
					"and its review history (`CREATED`, `APPROVED`, `COMMENTED`, `CHANGES_REQUESTED`, `MERGED`, ...). " +
					"`mergeStatus` is recomputed asynchronously and can briefly lag the latest history event, " +
					"don't treat a mismatch between the two as an error."
	)
	StructuredEntityOutput<ChangeRequest> getChangeRequest(
			McpTransportContext context,
			@McpToolParam(name = "service", description = "Unique service slug") String slug,
			@McpToolParam(name = "number", description = "Change request number") Long number
	) {
		final Namespace namespace = (Namespace) context.get(KonfigyrClaimNames.NAMESPACE);
		final Service service = services.get(namespace, slug).orElseThrow();

		return changeRequests.get(service, number)
				.map(StructuredOutput::of)
				.orElseThrow();
	}

}
