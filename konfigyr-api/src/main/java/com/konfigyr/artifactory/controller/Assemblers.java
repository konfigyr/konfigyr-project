package com.konfigyr.artifactory.controller;

import com.konfigyr.artifactory.ArtifactCoordinates;
import com.konfigyr.artifactory.Owner;
import com.konfigyr.artifactory.PropertyDefinition;
import com.konfigyr.artifactory.VersionedArtifact;
import com.konfigyr.artifactory.ownership.GroupVerification;
import com.konfigyr.artifactory.ownership.VerificationChallenge;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkBuilder;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import org.springframework.http.HttpMethod;

interface Assemblers {

	static RepresentationModelAssembler<VersionedArtifact, EntityModel<VersionedArtifact>> artifact(ArtifactCoordinates coordinates) {
		return artifact -> EntityModel.of(artifact, linkBuilder(coordinates).selfRel())
				.add(linkBuilder(coordinates).method(HttpMethod.POST).rel("publish"))
				.add(linkBuilder(coordinates).method(HttpMethod.GET).rel("properties"));
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

	static RepresentationModelAssembler<VerificationChallenge, EntityModel<VerificationChallenge>> verificationChallenge(Owner owner, GroupVerification verification) {
		return challenge -> EntityModel.of(
				challenge,
				linkBuilder(owner, verification).path("challenges").selfRel()
		);
	}

	static LinkBuilder linkBuilder(ArtifactCoordinates coordinates) {
		return Link.builder()
				.path("artifacts")
				.path(coordinates.groupId())
				.path(coordinates.artifactId())
				.path(coordinates.version().get());
	}

	static LinkBuilder linkBuilder(Owner owner, GroupVerification verification) {
		return Link.builder()
				.path("namespaces")
				.path(owner.slug())
				.path("group-verifications")
				.path(verification.groupId());
	}
}
