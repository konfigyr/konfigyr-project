package com.konfigyr.vault.controller;

import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkBuilder;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.Service;
import com.konfigyr.vault.ChangeHistory;
import com.konfigyr.vault.Profile;
import org.springframework.http.HttpMethod;

record VaultAssembler(Namespace namespace, Service service) {

	RepresentationModelAssembler<Profile, EntityModel<Profile>> profile() {
		return profile -> EntityModel.of(profile, linkBuilder(profile).selfRel())
				.add(linkBuilder(profile).method(HttpMethod.PUT).rel("update"))
				.add(linkBuilder(profile).method(HttpMethod.DELETE).rel("delete"))
				.add(linkBuilder(profile).path("properties").method(HttpMethod.GET).rel("state"))
				.add(linkBuilder(profile).path("history").method(HttpMethod.GET).rel("change history"))
				.add(linkBuilder(profile).path("apply").method(HttpMethod.POST).rel("apply"));

	}

	<T> RepresentationModelAssembler<T, EntityModel<T>> properties() {
		return EntityModel::of;
	}
	RepresentationModelAssembler<ChangeHistory, EntityModel<ChangeHistory>> changeHistory(Profile profile) {
		return changeHistory -> EntityModel.of(changeHistory, linkBuilder(profile, changeHistory).selfRel());
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

	private LinkBuilder linkBuilder(Profile profile, ChangeHistory changeHistory) {
		return linkBuilder()
				.path("profiles")
				.path(profile.name())
				.path("history")
				.path(changeHistory.id());

	}

}
