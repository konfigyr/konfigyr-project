package com.konfigyr.security.access;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ObjectIdentityTest {

	@Test
	@DisplayName("object identities should be equal when type and id match")
	void assertEquality() {
		assertThat(ObjectIdentity.namespace("konfigyr"))
				.isEqualTo(new ObjectIdentity("namespace", "konfigyr"))
				.isNotEqualTo(new ObjectIdentity("namespace", "john-doe"))
				.isNotEqualTo(new ObjectIdentity("other-type", "konfigyr"));
	}

	@Test
	@DisplayName("object identities should have simple string representation")
	void assertStringRepresentation() {
		assertThat(ObjectIdentity.namespace("konfigyr"))
				.hasToString("ObjectIdentity(namespace:konfigyr)");
	}

	@Test
	@DisplayName("should fail to create namespace identity with blank slugs")
	void assertNamespaceIdentifier() {
		assertThatIllegalArgumentException().isThrownBy(() -> ObjectIdentity.namespace(null));
		assertThatIllegalArgumentException().isThrownBy(() -> ObjectIdentity.namespace(""));
		assertThatIllegalArgumentException().isThrownBy(() -> ObjectIdentity.namespace(" "));
	}

}
