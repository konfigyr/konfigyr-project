package com.konfigyr.artifactory.controller;

import com.konfigyr.artifactory.ArtifactCoordinates;
import com.konfigyr.artifactory.PropertyDefinition;
import com.konfigyr.artifactory.VersionedArtifact;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkBuilder;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import org.springframework.http.HttpMethod;

interface Assemblers {

	static RepresentationModelAssembler<VersionedArtifact, EntityModel<VersionedArtifact>> artifact(ArtifactCoordinates coordinates) {
		return artifact -> EntityModel.of(artifact, linkBuilder(coordinates).selfRel())
				.add(linkBuilder(coordinates).method(HttpMethod.POST).rel("release"))
				.add(linkBuilder(coordinates).method(HttpMethod.GET).rel("properties"));
	}

	static RepresentationModelAssembler<PropertyDefinition, EntityModel<PropertyDefinition>> property(ArtifactCoordinates coordinates) {
		return property -> EntityModel.of(property, linkBuilder(coordinates).path(property.id().serialize()).selfRel());
	}

	static LinkBuilder linkBuilder(ArtifactCoordinates coordinates) {
		return Link.builder()
				.path("artifacts")
				.path(coordinates.groupId())
				.path(coordinates.artifactId())
				.path(coordinates.version().get());
	}
}
