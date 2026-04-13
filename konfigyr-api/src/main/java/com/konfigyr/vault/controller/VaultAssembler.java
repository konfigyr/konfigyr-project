package com.konfigyr.vault.controller;

import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkBuilder;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.Service;
import com.konfigyr.vault.ChangeHistory;
import com.konfigyr.vault.ChangeRequest;
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

	RepresentationModelAssembler<ChangeRequest, EntityModel<ChangeRequest>> changeRequest() {
		return request -> EntityModel.of(request, linkBuilder(request).selfRel())
				.add(linkBuilder(request).method(HttpMethod.PUT).rel("update"))
				.add(linkBuilder(request).method(HttpMethod.DELETE).rel("discard changes"))
				.add(linkBuilder(request).path("history").rel("history"))
				.add(linkBuilder(request).path("changes").rel("property changes"))
				.add(linkBuilder(request).path("merge").method(HttpMethod.POST).rel("merge changes"))
				.add(linkBuilder(request).path("review").method(HttpMethod.POST).rel("submit review"));
	}

	<T> RepresentationModelAssembler<T, EntityModel<T>> of() {
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
				.path(changeHistory.revision());
	}

	private LinkBuilder linkBuilder(ChangeRequest request) {
		return linkBuilder()
				.path("changes")
				.path(request.number());
	}

}
