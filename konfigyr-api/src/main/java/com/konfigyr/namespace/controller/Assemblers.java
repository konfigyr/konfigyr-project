package com.konfigyr.namespace.controller;

import com.konfigyr.hateoas.*;
import com.konfigyr.namespace.Invitation;
import com.konfigyr.namespace.Member;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.Service;
import org.springframework.http.HttpMethod;

interface Assemblers {

	static RepresentationModelAssembler<Namespace, EntityModel<Namespace>> namespace() {
		return namespace -> EntityModel.of(namespace, linkBuilder(namespace).selfRel())
				.add(linkBuilder(namespace).method(HttpMethod.PUT).rel("update"))
				.add(linkBuilder(namespace).method(HttpMethod.DELETE).rel("delete"))
				.add(linkBuilder(namespace).path("invitations").rel("invitations"))
				.add(linkBuilder(namespace).path("members").rel("members"))
				.add(linkBuilder(namespace).path("services").rel("services"));
	}

	static RepresentationModelAssembler<Member, EntityModel<Member>> member() {
		return member -> EntityModel.of(member, linkBuilder(member).selfRel())
				.add(linkBuilder(member).method(HttpMethod.DELETE).rel("delete"))
				.add(linkBuilder(member).method(HttpMethod.PUT).rel("update"));
	}

	static RepresentationModelAssembler<Invitation, EntityModel<Invitation>> invitation() {
		return invitation -> EntityModel.of(invitation, linkBuilder(invitation).selfRel())
				.add(linkBuilder(invitation).method(HttpMethod.DELETE).rel("cancel"))
				.add(linkBuilder(invitation).method(HttpMethod.POST).rel("accept"));
	}

	static RepresentationModelAssembler<Service, EntityModel<Service>> service(Namespace namespace) {
		return service -> EntityModel.of(service, linkBuilder(namespace, service).selfRel())
				.add(linkBuilder(namespace, service).method(HttpMethod.DELETE).rel("delete"));
	}

	static LinkBuilder linkBuilder() {
		return Link.builder().path("namespaces");
	}

	static LinkBuilder linkBuilder(Namespace namespace) {
		return linkBuilder().path(namespace.slug());
	}

	static LinkBuilder linkBuilder(Member member) {
		return linkBuilder().path(member.namespace().serialize())
				.path("members")
				.path(member.id().serialize());
	}

	static LinkBuilder linkBuilder(Invitation invitation) {
		return linkBuilder().path(invitation.namespace().serialize())
				.path("invitations")
				.path(invitation.key());
	}

	static LinkBuilder linkBuilder(Namespace namespace, Service service) {
		return linkBuilder(namespace)
				.path("services")
				.path(service.slug());
	}
}
