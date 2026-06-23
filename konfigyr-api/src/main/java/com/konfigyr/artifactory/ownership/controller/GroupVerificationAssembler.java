package com.konfigyr.artifactory.ownership.controller;

import com.konfigyr.artifactory.ownership.GroupVerification;
import com.konfigyr.artifactory.ownership.VerificationChallenge;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkBuilder;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import org.springframework.http.HttpMethod;

record GroupVerificationAssembler(String namespace) {

	RepresentationModelAssembler<GroupVerification, EntityModel<GroupVerification>> groupVerification() {
		return verification -> EntityModel.of(verification, linkBuilder(verification).selfRel())
				.add(linkBuilder(verification).method(HttpMethod.POST).path("verify").rel("verify"))
				.add(linkBuilder(verification).method(HttpMethod.DELETE).rel("revoke"));
	}

	RepresentationModelAssembler<VerificationChallenge, EntityModel<VerificationChallenge>> verificationChallenge() {
		return verification -> EntityModel.of(verification, linkBuilder(verification).selfRel());
	}

	private LinkBuilder linkBuilder(GroupVerification verification) {
		return linkBuilder().path(verification.groupId());
	}

	private LinkBuilder linkBuilder(VerificationChallenge verification) {
		return linkBuilder()
				.path(verification.verificationId().serialize())
				.path("verification-challenges");
	}

	private LinkBuilder linkBuilder() {
		return Link.builder()
				.path("namespaces")
				.path(namespace)
				.path("group-verifications");
	}
}
