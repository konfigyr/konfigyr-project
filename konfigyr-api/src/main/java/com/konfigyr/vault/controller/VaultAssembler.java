package com.konfigyr.vault.controller;

import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkBuilder;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.Service;
import com.konfigyr.vault.Profile;
import org.springframework.http.HttpMethod;

record VaultAssembler(Namespace namespace, Service service) {

	RepresentationModelAssembler<Profile, EntityModel<Profile>> profile() {
		return profile -> EntityModel.of(profile, linkBuilder(profile).selfRel())
				.add(linkBuilder(profile).method(HttpMethod.PUT).rel("update"))
				.add(linkBuilder(profile).method(HttpMethod.DELETE).rel("delete"));
	}

	private LinkBuilder linkBuilder() {
		return Link.builder().path("namespaces")
				.path(namespace.slug())
				.path("services")
				.path(service.slug());
	}

	private LinkBuilder linkBuilder(Profile profile) {
		return linkBuilder()
				.path("profiles")
				.path(profile.name());
	}

}
