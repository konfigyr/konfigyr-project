package com.konfigyr.registry;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

class RepositoryTest {

	@Test
	@DisplayName("should create repository using fluent builder")
	void shouldCreateRepository() {
		final var repository = Repository.builder()
				.id(12476518224L)
				.namespace("konfigyr")
				.slug("konfigyr-crypto-api")
				.name("Konfigyr Crypto API")
				.description("Spring Boot Crypto API library")
				.isPrivate(false)
				.createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
				.updatedAt(Instant.now().minus(3, ChronoUnit.HOURS))
				.build();

		assertThat(repository)
				.returns(EntityId.from(12476518224L), Repository::id)
				.returns("konfigyr", Repository::namespace)
				.returns("konfigyr-crypto-api", Repository::slug)
				.returns("Konfigyr Crypto API", Repository::name)
				.returns("Spring Boot Crypto API library", Repository::description)
				.returns(false, Repository::isPrivate)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10), within(1, ChronoUnit.SECONDS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(ZoneOffset.UTC).minusHours(3), within(1, ChronoUnit.SECONDS))
				);
	}

	@Test
	@DisplayName("should validate repository data when using fluent builder")
	void shouldValidateRepositoryBuilder() {
		final var builder = Repository.builder();

		assertThatThrownBy(builder::build)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Repository entity identifier can not be null");

		assertThatThrownBy(() -> builder.id("000000BKTH3TG").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Namespace slug can not be blank");

		assertThatThrownBy(() -> builder.namespace("konfigyr").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Repository slug can not be blank");

		assertThatThrownBy(() -> builder.slug("konfigyr-crypto-api").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Repository name can not be blank");

		assertThat(builder.name("Konfigyr Crypto API").build())
				.returns(EntityId.from(12476518224L), Repository::id)
				.returns("konfigyr", Repository::namespace)
				.returns("konfigyr-crypto-api", Repository::slug)
				.returns("Konfigyr Crypto API", Repository::name)
				.returns(null, Repository::description)
				.returns(false, Repository::isPrivate)
				.returns(null, Repository::createdAt)
				.returns(null, Repository::updatedAt);
	}

}