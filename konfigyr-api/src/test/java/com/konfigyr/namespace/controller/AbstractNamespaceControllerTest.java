package com.konfigyr.namespace.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.MemberNotFoundException;
import com.konfigyr.namespace.NamespaceNotFoundException;
import com.konfigyr.namespace.UnsupportedMembershipOperationException;
import com.konfigyr.test.AbstractControllerTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.function.Consumer;

abstract class AbstractNamespaceControllerTest extends AbstractControllerTest {

	static Consumer<MvcTestResult> operationNotSupported() {
		return problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
				.hasTitle("Operation not allowed")
				.hasDetailContaining("This action can't be completed because it would leave the organization without an administrator")
		).andThen(hasFailedWithException(UnsupportedMembershipOperationException.class));
	}

	static Consumer<MvcTestResult> namespaceNotFound(String slug) {
		return problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
				.hasTitle("Organization not found")
				.hasDetailContaining("The namespace you're trying to access doesn't exist or is no longer available.")
		).andThen(hasFailedWithException(NamespaceNotFoundException.class, ex -> ex
				.hasMessageContaining("Could not find a namespace with the following name: " + slug)
		));
	}

	static Consumer<MvcTestResult> memberNotFound(long id) {
		return problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
				.hasTitle("Member not found")
				.hasDetailContaining("We couldn't find a member matching your request.")
		).andThen(hasFailedWithException(MemberNotFoundException.class, ex -> ex
				.hasMessageContaining(EntityId.from(id).serialize())
		));
	}

}
