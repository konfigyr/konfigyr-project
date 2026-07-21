package com.konfigyr.artifactory.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.konfigyr.artifactory.*;
import com.konfigyr.artifactory.ownership.GroupVerification;
import com.konfigyr.artifactory.ownership.VerificationChallenge;
import com.konfigyr.artifactory.transfer.ArtifactOwnershipTransfer;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkBuilder;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import org.springframework.http.HttpMethod;

import java.util.Set;
import java.util.stream.Collectors;

interface Assemblers {

	static RepresentationModelAssembler<ArtifactDefinition, EntityModel<ArtifactDefinition>> definition() {
		return definition -> EntityModel.of(definition, linkBuilder(definition).selfRel());
	}

	static RepresentationModelAssembler<ArtifactDefinition, EntityModel<ArtifactDefinition>> definition(Owner owner) {
		return definition -> EntityModel.of(definition, linkBuilder(owner, definition).selfRel())
				.add(linkBuilder(owner, definition).path("versions").rel("List artifact versions"))
				.add(linkBuilder(owner, definition).path("visibility").method(HttpMethod.PUT).rel("Update artifact visibility"))
				.add(linkBuilder(owner, definition).method(HttpMethod.DELETE).rel("Deregister artifact"));
	}

	static RepresentationModelAssembler<VersionedArtifact, EntityModel<VersionedArtifact>> artifact(ArtifactCoordinates coordinates) {
		return artifact -> EntityModel.of(artifact, linkBuilder(coordinates).selfRel())
				.add(linkBuilder(coordinates).method(HttpMethod.POST).rel("publish"))
				.add(linkBuilder(coordinates).method(HttpMethod.GET).rel("properties"));
	}

	static RepresentationModelAssembler<VersionedArtifact, EntityModel<VersionedArtifact>> artifact(Owner owner) {
		return artifact -> EntityModel.of(artifact, linkBuilder(owner, (Artifact) artifact).selfRel())
				.add(linkBuilder(owner, (Artifact) artifact).method(HttpMethod.DELETE).rel("Retract artifact version"));
	}

	static RepresentationModelAssembler<PropertyDefinition, EntityModel<PropertyDefinition>> property() {
		return EntityModel::of;
	}

	static RepresentationModelAssembler<PropertyDefinition, EntityModel<PropertyDefinition>> property(ArtifactCoordinates coordinates) {
		return property -> EntityModel.of(property, linkBuilder(coordinates).path(property.id().serialize()).selfRel());
	}

	static RepresentationModelAssembler<GroupVerification, EntityModel<GroupVerification>> groupVerification(Owner owner) {
		return verification -> EntityModel.of(verification, linkBuilder(owner, verification).selfRel())
				.add(linkBuilder(owner, verification).method(HttpMethod.GET).path("challenges").rel("list verification challenges"))
				.add(linkBuilder(owner, verification).method(HttpMethod.POST).path("verify").rel("verify"))
				.add(linkBuilder(owner, verification).method(HttpMethod.DELETE).rel("revoke"));
	}

	static RepresentationModelAssembler<GroupVerification, EntityModel<GroupVerificationRepresentation>> groupVerification(Owner owner, Set<Owner> conflictingOwners) {
		final Set<String> conflicts = conflictingOwners.stream().map(Owner::slug).collect(Collectors.toUnmodifiableSet());

		return verification -> EntityModel.of(new GroupVerificationRepresentation(verification, conflicts), linkBuilder(owner, verification).selfRel())
				.add(linkBuilder(owner, verification).method(HttpMethod.GET).path("challenges").rel("list verification challenges"))
				.add(linkBuilder(owner, verification).method(HttpMethod.POST).path("verify").rel("verify"))
				.add(linkBuilder(owner, verification).method(HttpMethod.DELETE).rel("revoke"));
	}

	static RepresentationModelAssembler<VerificationChallenge, EntityModel<VerificationChallenge>> verificationChallenge(Owner owner, GroupVerification verification) {
		return challenge -> EntityModel.of(
				challenge,
				linkBuilder(owner, verification).path("challenges").selfRel()
		);
	}

	static RepresentationModelAssembler<ArtifactOwnershipTransfer, EntityModel<ArtifactOwnershipTransfer>> artifactOwnershipTransfer(Owner owner) {
		return transfer -> EntityModel.of(transfer, linkBuilder(owner, transfer).selfRel())
				.add(linkBuilder(owner, transfer).method(HttpMethod.POST).path("accept").rel("Accept ownership transfer request"))
				.add(linkBuilder(owner, transfer).method(HttpMethod.POST).path("reject").rel("Reject ownership transfer request"))
				.add(linkBuilder(owner, transfer).method(HttpMethod.DELETE).rel("Cancel ownership transfer request"));
	}

	static LinkBuilder linkBuilder(ArtifactKey key) {
		return Link.builder()
				.path("artifacts")
				.path(key.groupId())
				.path(key.artifactId());
	}

	static LinkBuilder linkBuilder(ArtifactCoordinates coordinates) {
		return linkBuilder((ArtifactKey) coordinates)
				.path(coordinates.version().get());
	}

	static LinkBuilder linkBuilder(Owner owner, ArtifactKey key) {
		return Link.builder()
				.path("namespaces")
				.path(owner.slug())
				.path("artifacts")
				.path(key.groupId())
				.path(key.artifactId());
	}

	static LinkBuilder linkBuilder(Owner owner, Artifact artifact) {
		return Link.builder()
				.path("namespaces")
				.path(owner.slug())
				.path("artifacts")
				.path(artifact.groupId())
				.path(artifact.artifactId())
				.path(artifact.version());
	}

	static LinkBuilder linkBuilder(Owner owner, GroupVerification verification) {
		return Link.builder()
				.path("namespaces")
				.path(owner.slug())
				.path("group-verifications")
				.path(verification.groupId());
	}

	static LinkBuilder linkBuilder(Owner owner, ArtifactOwnershipTransfer transfer) {
		return Link.builder()
				.path("namespaces")
				.path(owner.slug())
				.path("artifact-transfers")
				.path(transfer.id().serialize());
	}

	/**
	 * Enriched {@link GroupVerification} representation returned by the {@code claim} and {@code verify}
	 * endpoints, adding the {@code conflictingOwners} field alongside the verification's own fields.
	 *
	 * @param verification the claimed or verified {@link GroupVerification}
	 * @param conflictingOwners slugs of namespaces, other than the caller, that already own artifacts under
	 *        {@link GroupVerification#groupId()}
	 */
	record GroupVerificationRepresentation(
			@JsonUnwrapped GroupVerification verification,
			@JsonInclude(JsonInclude.Include.NON_EMPTY) Set<String> conflictingOwners
	) { }
}
