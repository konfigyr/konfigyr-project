package com.konfigyr.membership;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.support.Avatar;
import com.konfigyr.support.FullName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

class MemberTest {

	@Test
	@DisplayName("should create namespace member using fluent builder")
	void shouldCreateNamespaceMember() {
		final var member = Member.builder()
				.id(8365L)
				.namespace(972L)
				.account(72L)
				.role(NamespaceRole.ADMIN)
				.email("john.doe@konfigyr.com")
				.fullName("John Doe")
				.avatar("https://example.com/avatar.svg")
				.since(Instant.now().minus(17, ChronoUnit.DAYS))
				.build();

		assertThat(member)
				.returns(EntityId.from(8365), Member::id)
				.returns(EntityId.from(972), Member::namespace)
				.returns(EntityId.from(72), Member::account)
				.returns(NamespaceRole.ADMIN, Member::role)
				.returns("john.doe@konfigyr.com", Member::email)
				.returns("John", Member::firstName)
				.returns("Doe", Member::lastName)
				.returns("John Doe", Member::displayName)
				.returns(FullName.of("John", "Doe"), Member::fullName)
				.returns(Avatar.parse("https://example.com/avatar.svg"), Member::avatar)
				.satisfies(it -> assertThat(it.since())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(ZoneOffset.UTC).minusDays(17), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should validate namespace member data when using fluent builder")
	void shouldValidateNamespaceMemberBuilder() {
		final var builder = Member.builder();

		assertThatThrownBy(builder::build)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Member entity identifier can not be null");

		assertThatThrownBy(() -> builder.id("000000BKTH3TG").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace entity identifier can not be null");

		assertThatThrownBy(() -> builder.namespace("000000BKTH3TG").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Account entity identifier can not be null");

		assertThatThrownBy(() -> builder.account("000000BKTH3TG").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace role can not be null");

		assertThatThrownBy(() -> builder.role("USER").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Member email address can not be blank");

		assertThatThrownBy(() -> builder.email("jane.doe@konfigyr.com").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Member full name can not be null");

		assertThat(builder.fullName("Jane Doe").build())
				.returns(EntityId.from(12476518224L), Member::id)
				.returns(EntityId.from(12476518224L), Member::namespace)
				.returns(EntityId.from(12476518224L), Member::account)
				.returns(NamespaceRole.USER, Member::role)
				.returns("jane.doe@konfigyr.com", Member::email)
				.returns(FullName.of("Jane", "Doe"), Member::fullName)
				.returns("Jane Doe", Member::displayName)
				.returns("Jane", Member::firstName)
				.returns("Doe", Member::lastName)
				.returns(Avatar.generate(EntityId.from(12476518224L), "JD"), Member::avatar)
				.returns(null, Member::since);
	}

}
