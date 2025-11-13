package com.konfigyr.security.access;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AccessControlTest {

	MutableAccessControl control;

	@BeforeEach
	void setup() {
		control = new KonfigyrAccessControl("testing-object", "testing", Set.of(
				new AccessGrant(securityIdentityFor(1), "read"),
				new AccessGrant(securityIdentityFor(2), "read"),
				new AccessGrant(securityIdentityFor(2), "write")
		));
	}

	@Test
	@DisplayName("should grant access when security identifier has a matching permission")
	void shouldGrantAccess() {
		assertThat(control.isGranted(securityIdentityFor(1), Set.of("read"))).isTrue();
		assertThat(control.isGranted(securityIdentityFor(1), Set.of("write"))).isFalse();

		assertThat(control.isGranted(securityIdentityFor(2), Set.of("read"))).isTrue();
		assertThat(control.isGranted(securityIdentityFor(2), Set.of("write"))).isTrue();

		assertThat(control.isGranted(securityIdentityFor(3), Set.of("read"))).isFalse();
		assertThat(control.isGranted(securityIdentityFor(3), Set.of("write"))).isFalse();
	}

	@Test
	@DisplayName("should grant access when security identifier has at least one permission")
	void shouldGrantAccessForAtLeastOnePermission() {
		assertThat(control.isGranted(securityIdentityFor(1), Set.of("read", "write"))).isTrue();

		assertThat(control.isGranted(securityIdentityFor(2), Set.of("read", "write"))).isTrue();

		assertThat(control.isGranted(securityIdentityFor(3), Set.of("read", "write"))).isFalse();
	}

	@Test
	@DisplayName("should not grant access with empty permissions")
	void emptyPermissions() {
		assertThat(control.isGranted(securityIdentityFor(1), Set.of())).isFalse();
		assertThat(control.isGranted(securityIdentityFor(2), Set.of())).isFalse();
		assertThat(control.isGranted(securityIdentityFor(3), Set.of())).isFalse();
	}

	@Test
	@DisplayName("should add access grant")
	void addGrants() {
		control.add(new AccessGrant(securityIdentityFor(1), "write"));

		assertThat(control)
				.hasSize(4)
				.containsExactlyInAnyOrder(
						new AccessGrant(securityIdentityFor(1), "read"),
						new AccessGrant(securityIdentityFor(1), "write"),
						new AccessGrant(securityIdentityFor(2), "read"),
						new AccessGrant(securityIdentityFor(2), "write")
				);
	}

	@Test
	@DisplayName("should remove access grant")
	void removeGrants() {
		control.remove(new AccessGrant(securityIdentityFor(1), "read"));

		assertThat(control)
				.hasSize(2)
				.containsExactlyInAnyOrder(
						new AccessGrant(securityIdentityFor(2), "read"),
						new AccessGrant(securityIdentityFor(2), "write")
				);
	}

	@Test
	@DisplayName("should remove grants for security identifier")
	void removeGrantsForSecurityIdentifier() {
		control.remove(securityIdentityFor(2));

		assertThat(control)
				.hasSize(1)
				.containsExactlyInAnyOrder(
						new AccessGrant(securityIdentityFor(1), "read")
				);
	}

	static SecurityIdentity securityIdentityFor(long id) {
		return SecurityIdentity.account(EntityId.from(id));
	}

}
