package com.konfigyr.vault;

import com.konfigyr.data.PageableExecutor;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.*;
import com.konfigyr.support.SearchQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.konfigyr.data.tables.Services.SERVICES;
import static com.konfigyr.data.tables.VaultProfiles.VAULT_PROFILES;

@Slf4j
@NullMarked
@RequiredArgsConstructor
class DefaultProfileManager implements ProfileManager {

	private final Marker CREATED = MarkerFactory.getMarker("PROFILE_CREATED");
	private final Marker UPDATED = MarkerFactory.getMarker("PROFILE_UPDATED");
	private final Marker DELETED = MarkerFactory.getMarker("PROFILE_DELETED");

	private static final Set<Field<?>> PROFILE_FIELDS = Set.of(
			VAULT_PROFILES.ID,
			VAULT_PROFILES.SERVICE_ID,
			VAULT_PROFILES.SLUG,
			VAULT_PROFILES.NAME,
			VAULT_PROFILES.DESCRIPTION,
			VAULT_PROFILES.POLICY,
			VAULT_PROFILES.POSITION,
			VAULT_PROFILES.CREATED_AT,
			VAULT_PROFILES.UPDATED_AT
	);

	static final PageableExecutor profilesExecutor = PageableExecutor.builder()
			.defaultSortField(VAULT_PROFILES.POSITION.asc())
			.sortField("name", VAULT_PROFILES.NAME)
			.sortField("date", VAULT_PROFILES.UPDATED_AT)
			.sortField("position", VAULT_PROFILES.POSITION)
			.build();

	private final DSLContext context;
	private final ApplicationEventPublisher publisher;

	@Override
	@Transactional(label = "vault.profile-search", readOnly = true)
	public Page<Profile> find(Service service, SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();
		conditions.add(VAULT_PROFILES.SERVICE_ID.eq(service.id().get()));

		query.term().map(term -> "%" + term + "%").ifPresent(term -> conditions.add(DSL.or(
				VAULT_PROFILES.SLUG.likeIgnoreCase(term),
				VAULT_PROFILES.NAME.likeIgnoreCase(term),
				VAULT_PROFILES.DESCRIPTION.likeIgnoreCase(term)
		)));

		if (log.isDebugEnabled()) {
			log.debug("Fetching profiles for conditions: {}", conditions);
		}

		return profilesExecutor.execute(
				createProfilesQuery(DSL.and(conditions)),
				DefaultProfileManager::toProfile,
				query.pageable(),
				() -> context.fetchCount(createProfilesQuery(DSL.and(conditions)))
		);
	}

	@Override
	@Transactional(label = "vault.profile-retrieve", readOnly = true)
	public Optional<Profile> get(EntityId id) {
		return fetchProfile(VAULT_PROFILES.ID.eq(id.get()));
	}

	@Override
	@Transactional(label = "vault.profile-retrieve", readOnly = true)
	public Optional<Profile> get(Service service, String name) {
		return fetchProfile(DSL.and(
				VAULT_PROFILES.SERVICE_ID.eq(service.id().get()),
				VAULT_PROFILES.SLUG.eq(name)
		));
	}

	@Override
	@Transactional(label = "vault.profile-exists", readOnly = true)
	public boolean exists(Service service, String name) {
		return context.fetchExists(VAULT_PROFILES, DSL.and(
				VAULT_PROFILES.SERVICE_ID.eq(service.id().get()),
				VAULT_PROFILES.SLUG.eq(name)
		));
	}

	@Override
	@Transactional(label = "vault.profile-create")
	public Profile create(ProfileDefinition definition) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to create profile from: {}", definition);
		}

		assertServiceExists(definition.service());

		final Profile profile;

		try {
			profile = context.insertInto(VAULT_PROFILES)
					.set(
							SettableRecord.of(context, VAULT_PROFILES)
									.set(VAULT_PROFILES.ID, EntityId.generate().map(EntityId::get))
									.set(VAULT_PROFILES.SERVICE_ID, definition.service().get())
									.set(VAULT_PROFILES.SLUG, definition.slug().get())
									.set(VAULT_PROFILES.NAME, definition.name())
									.set(VAULT_PROFILES.DESCRIPTION, definition.description())
									.set(VAULT_PROFILES.POLICY, definition.policy().name())
									.set(VAULT_PROFILES.STATE, "INITIALIZING")
									.set(VAULT_PROFILES.POSITION, definition.position())
									.set(VAULT_PROFILES.CREATED_AT, OffsetDateTime.now())
									.set(VAULT_PROFILES.UPDATED_AT, OffsetDateTime.now())
									.get()
					)
					.returning(PROFILE_FIELDS)
					.fetchOne(DefaultProfileManager::toProfile);
		} catch (DuplicateKeyException e) {
			throw new ProfileExistsException(definition, e);
		} catch (Exception e) {
			throw new NamespaceException("Unexpected exception occurred while creating a profile", e);
		}

		Assert.state(profile != null, () -> "Could not create profile from: " + definition);

		log.info(CREATED, "Successfully created new profile {} from {}", profile.id(), definition);

		publisher.publishEvent(new ProfileEvent.Created(profile));

		return profile;
	}

	@Override
	@Transactional(label = "vault.profile-update")
	public Profile update(EntityId id, ProfileDefinition definition) {
		try {
			context.update(VAULT_PROFILES)
					.set(VAULT_PROFILES.NAME, definition.name())
					.set(VAULT_PROFILES.DESCRIPTION, definition.description())
					.set(VAULT_PROFILES.POLICY, definition.policy().name())
					.set(VAULT_PROFILES.POSITION, definition.position())
					.set(VAULT_PROFILES.UPDATED_AT, OffsetDateTime.now())
					.where(VAULT_PROFILES.ID.eq(id.get()))
					.execute();
		} catch (Exception e) {
			throw new NamespaceException("Unexpected exception occurred while updating profile", e);
		}

		final Profile profile = fetchProfile(VAULT_PROFILES.ID.eq(id.get()))
				.orElseThrow(() -> new ProfileNotFoundException(id));

		log.info(UPDATED, "Successfully updated profile {} from {}", profile.id(), definition);

		publisher.publishEvent(new ProfileEvent.Updated(profile));

		return profile;
	}

	@Override
	@Transactional(label = "vault.profile-delete")
	public void delete(EntityId id) {
		delete(get(id).orElseThrow(() -> new ProfileNotFoundException(id)));
	}

	@Override
	@Transactional(label = "vault.profile-delete")
	public void delete(Service service, String name) {
		delete(get(service, name).orElseThrow(() -> new ProfileNotFoundException(service.slug(), name)));
	}

	private Optional<Profile> fetchProfile(Condition condition) {
		return createProfilesQuery(condition).fetchOptional(DefaultProfileManager::toProfile);
	}

	private void delete(Profile profile) {
		final long count = context.delete(VAULT_PROFILES)
				.where(VAULT_PROFILES.ID.eq(profile.id().get()))
				.execute();

		Assert.state(count != 0, "Failed to delete Profile with identifier: " + profile.id());

		log.info(DELETED, "Successfully deleted profile {}", profile.id());

		publisher.publishEvent(new ProfileEvent.Deleted(profile));
	}

	private SelectConditionStep<Record> createProfilesQuery(Condition condition) {
		return context.select(PROFILE_FIELDS)
				.from(VAULT_PROFILES)
				.where(condition);
	}

	private void assertServiceExists(EntityId service) {
		if (!context.fetchExists(SERVICES, SERVICES.ID.eq(service.get()))) {
			throw new ServiceNotFoundException(service);
		}
	}

	static Profile toProfile(Record record) {
		return Profile.builder()
				.id(record.get(VAULT_PROFILES.ID, EntityId.class))
				.service(record.get(VAULT_PROFILES.SERVICE_ID, EntityId.class))
				.slug(record.get(VAULT_PROFILES.SLUG))
				.name(record.get(VAULT_PROFILES.NAME))
				.description(record.get(VAULT_PROFILES.DESCRIPTION))
				.policy(record.get(VAULT_PROFILES.POLICY, ProfilePolicy.class))
				.position(record.get(VAULT_PROFILES.POSITION))
				.createdAt(record.get(VAULT_PROFILES.CREATED_AT))
				.updatedAt(record.get(VAULT_PROFILES.UPDATED_AT))
				.build();
	}
}
