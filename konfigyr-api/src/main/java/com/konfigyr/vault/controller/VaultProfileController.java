package com.konfigyr.vault.controller;

import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.Services;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.vault.Profile;
import com.konfigyr.vault.ProfileDefinition;
import com.konfigyr.vault.ProfileManager;
import com.konfigyr.vault.ProfilePolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Length;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/namespaces/{namespace}/services/{service}")
class VaultProfileController extends AbstractVaultController {

	VaultProfileController(NamespaceManager namespaces, Services services, ProfileManager profiles) {
		super(namespaces, profiles, services);
	}

	@GetMapping("profiles")
	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.READ_PROFILES)
	PagedModel<EntityModel<Profile>> search(
			@PathVariable String namespace,
			@PathVariable String service,
			@RequestParam(required = false) String term,
			Pageable pageable
	) {
		final VaultAssembler assembler = createAssembler(namespace, service);

		final SearchQuery query = SearchQuery.builder()
				.term(term)
				.pageable(pageable)
				.build();

		return assembler.profile().assemble(profiles.find(assembler.service(), query));
	}

	@PostMapping("profiles")
	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.WRITE_PROFILES)
	EntityModel<Profile> create(
			@PathVariable String namespace,
			@PathVariable String service,
			@RequestBody @Validated CreateProfileRequest request
	) {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final Profile profile = profiles.create(request.definition(assembler.service()));

		return assembler.profile().assemble(profile);
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.READ_PROFILES)
	@RequestMapping(path = "profiles/{profile}", method = RequestMethod.HEAD)
	ResponseEntity<@NonNull Void> check(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable String profile
	) {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final HttpStatus status = profiles.exists(assembler.service(), profile) ? HttpStatus.OK : HttpStatus.NOT_FOUND;
		return ResponseEntity.status(status).build();
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.READ_PROFILES)
	@GetMapping("profiles/{profileName}")
	EntityModel<Profile> lookup(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable String profileName
	) {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final Profile profile = lookupProfile(assembler.service(), profileName);

		return assembler.profile().assemble(profile);
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.WRITE_PROFILES)
	@GetMapping("profiles/{profileName}/history")
	EntityModel<Profile> history(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable String profileName
	) {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final Profile profile = lookupProfile(assembler.service(), profileName);

		return assembler.profile().assemble(profile);
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.WRITE_PROFILES)
	@PutMapping("profiles/{profileName}")
	EntityModel<Profile> update(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable String profileName,
			@RequestBody @Validated UpdateProfileRequest request
	) {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final Profile profile = lookupProfile(assembler.service(), profileName);

		return assembler.profile().assemble(
				profiles.update(profile.id(), request.definition(assembler.service(), profile))
		);
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.DELETE_PROFILES)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("profiles/{profileName}")
	void delete(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable String profileName
	) {
		final VaultAssembler assembler = createAssembler(namespace, service);
		profiles.delete(assembler.service(), profileName);
	}

	record CreateProfileRequest(
			@NotBlank @Length(min = 2, max = 30) String name,
			@NotBlank @Length(min = 2, max = 30) String slug,
			@Length(max = 255) String description,
			@NotNull ProfilePolicy policy,
			@Positive Integer position
	) {
		ProfileDefinition definition(Service service) {
			return ProfileDefinition.builder()
					.service(service.id())
					.slug(slug)
					.name(name)
					.description(description)
					.policy(policy)
					.position(position == null ? 1 : position)
					.build();
		}
	}

	record UpdateProfileRequest(
			@Length(min = 2, max = 30) String name,
			@Length(max = 255) String description,
			ProfilePolicy policy,
			@Positive Integer position
	) {
		ProfileDefinition definition(Service service, Profile profile) {
			return ProfileDefinition.builder()
					.service(service.id())
					.slug(profile.slug())
					.name(name == null ? profile.name() : name)
					.description(description == null ? profile.description() : description)
					.policy(policy == null ? profile.policy() : policy)
					.position(position == null ? profile.position() : position)
					.build();
		}
	}

}
