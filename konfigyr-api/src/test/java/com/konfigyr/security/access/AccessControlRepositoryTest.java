package com.konfigyr.security.access;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.test.AbstractIntegrationTest;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

class AccessControlRepositoryTest extends AbstractIntegrationTest {

	@Autowired
	AccessControlRepository repository;

	@Autowired
	TestObservationRegistry observationRegistry;

	@Test
	@DisplayName("should fail to load access control for unknown object type")
	void unknownObjectType() {
		final var identity = new ObjectIdentity("unknown", "id");

		assertThatExceptionOfType(ObjectIdentityNotFound.class)
				.isThrownBy(() -> repository.get(identity))
				.returns(identity, ObjectIdentityNotFound::getObjectIdentity)
				.withNoCause();

		assertThatObservation()
				.hasError()
				.hasLowCardinalityKeyValue("konfigyr.identity.type", "unknown")
				.hasHighCardinalityKeyValue("konfigyr.identity.id", "id")
				.assertThatError()
				.isInstanceOf(ObjectIdentityNotFound.class);
	}

	@Test
	@DisplayName("should fail to load access control for unknown identity")
	void unknownObjectIdentity() {
		final var identity = ObjectIdentity.namespace("unknown");

		assertThatExceptionOfType(ObjectIdentityNotFound.class)
				.isThrownBy(() -> repository.get(identity))
				.returns(identity, ObjectIdentityNotFound::getObjectIdentity)
				.withNoCause();

		assertThatObservation()
				.hasError()
				.hasLowCardinalityKeyValue("konfigyr.identity.type", ObjectIdentity.NAMESPACE_TYPE)
				.hasHighCardinalityKeyValue("konfigyr.identity.id", "unknown")
				.assertThatError()
				.isInstanceOf(ObjectIdentityNotFound.class);
	}

	@Test
	@DisplayName("should load access control for namespace using slug")
	void accessControlsForNamespaceViaSlug() {
		final var identity = ObjectIdentity.namespace("konfigyr");

		assertThatObject(repository.get(identity))
				.returns(identity, AccessControl::objectIdentity)
				.asInstanceOf(InstanceOfAssertFactories.iterable(AccessGrant.class))
				.containsExactlyInAnyOrder(
						AccessGrant.forNamespaceMember(EntityId.from(1), NamespaceRole.ADMIN),
						AccessGrant.forNamespaceMember(EntityId.from(2), NamespaceRole.USER),
						AccessGrant.forNamespaceApplication("kfg-A2c7mvoxEP1rb-_NQLvaZ5KJNTGR-oOp")
				);

		assertThatObservation()
				.doesNotHaveError()
				.hasLowCardinalityKeyValue("konfigyr.identity.type", ObjectIdentity.NAMESPACE_TYPE)
				.hasHighCardinalityKeyValue("konfigyr.identity.id", "konfigyr");
	}

	@Test
	@DisplayName("should load access control for namespace using entity identifier")
	void accessControlsForNamespaceViaEntityIdentifier() {
		final var identity = new ObjectIdentity(ObjectIdentity.NAMESPACE_TYPE, EntityId.from(1L));

		assertThatObject(repository.get(identity))
				.returns(identity, AccessControl::objectIdentity)
				.asInstanceOf(InstanceOfAssertFactories.iterable(AccessGrant.class))
				.containsExactlyInAnyOrder(
						AccessGrant.forNamespaceMember(EntityId.from(1), NamespaceRole.ADMIN),
						AccessGrant.forNamespaceApplication("kfg-A2c7mvoxEP346BQCSuwnJ5ZNQIEsgCBG"),
						AccessGrant.forNamespaceApplication("kfg-BAQp6u2ElYmuPyoa2Hj766ju0PPvL2Iq")
				);

		assertThatObservation()
				.doesNotHaveError()
				.hasLowCardinalityKeyValue("konfigyr.identity.type", ObjectIdentity.NAMESPACE_TYPE)
				.hasHighCardinalityKeyValue("konfigyr.identity.id", EntityId.from(1L).serialize());
	}

	@Test
	@DisplayName("should load access control for namespace using number")
	void accessControlsForNamespaceViaNumber() {
		final var identity = new ObjectIdentity(ObjectIdentity.NAMESPACE_TYPE, 1L);

		assertThatObject(repository.get(identity))
				.returns(identity, AccessControl::objectIdentity)
				.asInstanceOf(InstanceOfAssertFactories.iterable(AccessGrant.class))
				.containsExactlyInAnyOrder(
						AccessGrant.forNamespaceMember(EntityId.from(1), NamespaceRole.ADMIN),
						AccessGrant.forNamespaceApplication("kfg-A2c7mvoxEP346BQCSuwnJ5ZNQIEsgCBG"),
						AccessGrant.forNamespaceApplication("kfg-BAQp6u2ElYmuPyoa2Hj766ju0PPvL2Iq")
				);

		assertThatObservation()
				.doesNotHaveError()
				.hasLowCardinalityKeyValue("konfigyr.identity.type", ObjectIdentity.NAMESPACE_TYPE)
				.hasHighCardinalityKeyValue("konfigyr.identity.id", "1");
	}

	TestObservationRegistryAssert.TestObservationRegistryAssertReturningObservationContextAssert assertThatObservation() {
		return assertThat(observationRegistry)
				.hasObservationWithNameEqualTo("konfigyr.security.access-control")
				.that()
				.hasBeenStarted()
				.hasBeenStopped();
	}

}
