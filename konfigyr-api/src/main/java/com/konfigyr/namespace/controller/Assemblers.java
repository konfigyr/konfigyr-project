package com.konfigyr.namespace.controller;

import com.konfigyr.artifactory.Manifest;
import com.konfigyr.artifactory.PropertyDescriptor;
import com.konfigyr.hateoas.*;
import com.konfigyr.namespace.*;
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

	static RepresentationModelAssembler<NamespaceApplication, EntityModel<NamespaceApplication>> application(Namespace namespace) {
		return application -> EntityModel.of(application, linkBuilder(namespace, application).selfRel())
				.add(linkBuilder(namespace, application).method(HttpMethod.PUT).rel("reset-secret"))
				.add(linkBuilder(namespace, application).method(HttpMethod.DELETE).rel("delete"));
	}

	static RepresentationModelAssembler<Service, EntityModel<Service>> service(Namespace namespace) {
		return service -> EntityModel.of(service, linkBuilder(namespace, service).selfRel())
				.add(linkBuilder(namespace, service).path("manifest").rel("manifest"))
				.add(linkBuilder(namespace, service).method(HttpMethod.POST).path("manifest").rel("release"))
				.add(linkBuilder(namespace, service).method(HttpMethod.DELETE).rel("delete"));
	}

	static RepresentationModelAssembler<Manifest, EntityModel<Manifest>> manifest(Namespace namespace, Service service) {
		return manifest -> EntityModel.of(manifest, linkBuilder(namespace, service).path("manifest").selfRel())
				.add(linkBuilder(namespace, service).path("manifest").method(HttpMethod.POST).rel("release"));
	}

	static RepresentationModelAssembler<ServiceCatalog, EntityModel<ServiceCatalog>> catalog(Namespace namespace) {
		return catalog -> EntityModel.of(catalog, linkBuilder(namespace, catalog).selfRel())
				.add(linkBuilder(namespace, catalog).path("search").rel("search configuration catalog"));
	}

	static RepresentationModelAssembler<PropertyDescriptor, EntityModel<PropertyDescriptor>> property(Namespace namespace, Service service) {
		return property -> EntityModel.of(property, linkBuilder(namespace, service, property).selfRel());
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

	static LinkBuilder linkBuilder(Namespace namespace, NamespaceApplication application) {
		return linkBuilder(namespace)
				.path("applications")
				.path(application.id().serialize());
	}

	static LinkBuilder linkBuilder(Namespace namespace, ServiceCatalog catalog) {
		return linkBuilder(namespace, catalog.service())
				.path("catalog");
	}

	static LinkBuilder linkBuilder(Namespace namespace, Service service, PropertyDescriptor descriptor) {
		return linkBuilder(namespace, service)
				.path("catalog")
				.path(descriptor.name());
	}
}
