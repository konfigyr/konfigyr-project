package com.konfigyr.membership.controller;

import com.konfigyr.hateoas.*;
import com.konfigyr.membership.Invitation;
import com.konfigyr.membership.Member;
import com.konfigyr.namespace.Namespace;
import org.springframework.http.HttpMethod;

interface Assemblers {

	/**
	 * Creates an assembler for namespace members.
	 *
	 * @param namespace the namespace context
	 * @return member assembler, never {@literal null}
	 */
	static RepresentationModelAssembler<Member, EntityModel<Member>> member(Namespace namespace) {
		return member -> EntityModel.of(member, memberLinkBuilder(namespace, member).selfRel())
				.add(memberLinkBuilder(namespace, member).method(HttpMethod.DELETE).rel("delete"))
				.add(memberLinkBuilder(namespace, member).method(HttpMethod.PUT).rel("update"));
	}

	/**
	 * Creates an assembler for invitations scoped to a namespace (admin view).
	 * The self-link points to the namespace-scoped invitation endpoint, and a cancel action is added.
	 *
	 * @param namespace the namespace context
	 * @return invitation assembler, never {@literal null}
	 */
	static RepresentationModelAssembler<Invitation, EntityModel<Invitation>> invitationForNamespace(Namespace namespace) {
		return invitation -> EntityModel.of(invitation, invitationLinkBuilder(namespace, invitation).selfRel())
				.add(invitationLinkBuilder(namespace, invitation).method(HttpMethod.DELETE).rel("cancel"));
	}

	/**
	 * Creates an assembler for invitations scoped to the authenticated account (recipient view).
	 * The self-link points to the account-scoped invitation endpoint with accept and decline actions.
	 *
	 * @return invitation assembler, never {@literal null}
	 */
	static RepresentationModelAssembler<Invitation, EntityModel<Invitation>> invitationForAccount() {
		return invitation -> EntityModel.of(invitation, invitationLinkBuilder(invitation).selfRel())
				.add(invitationLinkBuilder(invitation).method(HttpMethod.DELETE).rel("decline"))
				.add(invitationLinkBuilder(invitation).method(HttpMethod.POST).rel("accept"));
	}

	static LinkBuilder memberLinkBuilder(Namespace namespace, Member member) {
		return Link.builder()
				.path("namespaces")
				.path(namespace.slug())
				.path("members")
				.path(member.id().serialize());
	}

	static LinkBuilder invitationLinkBuilder(Namespace namespace, Invitation invitation) {
		return Link.builder()
				.path("namespaces")
				.path(namespace.slug())
				.path("invitations")
				.path(invitation.key());
	}

	static LinkBuilder invitationLinkBuilder(Invitation invitation) {
		return Link.builder()
				.path("account")
				.path("invitations")
				.path(invitation.key());
	}

}
