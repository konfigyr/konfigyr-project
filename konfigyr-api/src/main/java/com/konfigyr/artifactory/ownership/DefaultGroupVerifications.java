package com.konfigyr.artifactory.ownership;

import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.GroupVerificationChallenges.GROUP_VERIFICATION_CHALLENGES;
import static com.konfigyr.data.tables.GroupVerifications.GROUP_VERIFICATIONS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

@Slf4j
@NullMarked
@RequiredArgsConstructor
class DefaultGroupVerifications implements GroupVerifications {

	private final DSLContext context;

	private final VerificationStrategy dnsTxtVerificationStrategy;

	private static final SecureRandom RANDOM = new SecureRandom();

	@Override
	@Transactional(readOnly = true, label = "group-verifications.find-active-covering")
	public Optional<GroupVerification> findActiveCovering(String groupId, Owner owner) {
		log.debug("Looking up active verification covering groupId '{}' for owner {} ({})", groupId, owner.slug(), owner.id());

		return context.select(GROUP_VERIFICATIONS.asterisk(), NAMESPACES.SLUG)
				.from(GROUP_VERIFICATIONS)
				.join(NAMESPACES)
				.on(GROUP_VERIFICATIONS.NAMESPACE_ID.eq(NAMESPACES.ID))
				.where(GROUP_VERIFICATIONS.NAMESPACE_ID.eq(owner.id().get()))
				.and(GROUP_VERIFICATIONS.STATE.eq(VerificationState.ACTIVE.name()))
				.and(prefixOf(groupId))
				.fetchOptional(DefaultGroupVerifications::toGroupVerification);
	}

	@Override
	@Transactional(readOnly = true, label = "group-verifications.find-any-overlapping")
	public Optional<GroupVerification> findAnyOverlapping(String groupId) {
		log.debug("Checking for any overlapping active verification with groupId '{}'", groupId);
		return context.select(GROUP_VERIFICATIONS.fields())
				.select(NAMESPACES.SLUG)
				.from(GROUP_VERIFICATIONS)
				.join(NAMESPACES).on(GROUP_VERIFICATIONS.NAMESPACE_ID.eq(NAMESPACES.ID))
				.where(GROUP_VERIFICATIONS.STATE.eq(VerificationState.ACTIVE.name()))
				.and(overlaps(groupId))
				.fetchOptional(DefaultGroupVerifications::toGroupVerification);
	}

	@Override
	@Transactional(readOnly = true, label = "group-verifications.find-by-owner")
	public List<GroupVerification> findByOwner(Owner owner) {
		log.debug("Listing group verifications for owner {} ({})", owner.slug(), owner.id());

		return context.select(GROUP_VERIFICATIONS.fields())
				.select(NAMESPACES.SLUG)
				.from(GROUP_VERIFICATIONS)
				.join(NAMESPACES).on(GROUP_VERIFICATIONS.NAMESPACE_ID.eq(NAMESPACES.ID))
				.where(GROUP_VERIFICATIONS.NAMESPACE_ID.eq(owner.id().get()))
				.fetch(DefaultGroupVerifications::toGroupVerification);
	}

	@Override
	@Transactional(readOnly = true, label = "group-verifications.find-by-group-id")
	public Optional<GroupVerification> findByGroupId(String groupId, Owner owner) {
		log.debug("Looking up group verification '{}' for owner {} ({})", groupId, owner.slug(), owner.id());
		return context.select(GROUP_VERIFICATIONS.fields())
				.select(NAMESPACES.SLUG)
				.from(GROUP_VERIFICATIONS)
				.join(NAMESPACES).on(GROUP_VERIFICATIONS.NAMESPACE_ID.eq(NAMESPACES.ID))
				.where(GROUP_VERIFICATIONS.NAMESPACE_ID.eq(owner.id().get()))
				.and(GROUP_VERIFICATIONS.GROUP_ID.eq(groupId))
				.fetchOptional(DefaultGroupVerifications::toGroupVerification);
	}

	@Override
	@Transactional(readOnly = true, label = "group-verifications.find-active-challenge")
	public Optional<VerificationChallenge> findActiveChallenge(GroupVerification verification) {
		log.debug("Looking up active challenge for verification {} with groupId '{}' and state {}", verification.id(), verification.groupId(), verification.state());
		return context.select(GROUP_VERIFICATION_CHALLENGES.fields())
				.from(GROUP_VERIFICATION_CHALLENGES)
				.where(GROUP_VERIFICATION_CHALLENGES.GROUP_VERIFICATION_ID.eq(verification.id().get()))
				.and(GROUP_VERIFICATION_CHALLENGES.STATE.eq(ChallengeState.UNVERIFIED.name()))
				.fetchOptional(DefaultGroupVerifications::toVerificationChallenge);
	}

	@Override
	@Transactional(readOnly = true, label = "group-verifications.find-challenges")
	public List<VerificationChallenge> findChallenges(EntityId verificationId, Owner owner) {
		log.debug("Listing challenges for verification {} and owner {} ({})", verificationId, owner.slug(), owner.id());
		return context.select(GROUP_VERIFICATION_CHALLENGES.fields())
				.from(GROUP_VERIFICATION_CHALLENGES)
				.join(GROUP_VERIFICATIONS).on(GROUP_VERIFICATION_CHALLENGES.GROUP_VERIFICATION_ID.eq(GROUP_VERIFICATIONS.ID))
				.where(GROUP_VERIFICATIONS.ID.eq(verificationId.get())).and(GROUP_VERIFICATIONS.NAMESPACE_ID.eq(owner.id().get()))
				.orderBy(GROUP_VERIFICATION_CHALLENGES.CREATED_AT.asc())
				.fetch(DefaultGroupVerifications::toVerificationChallenge);
	}

	@Override
	@Transactional(label = "group-verifications.claim")
	public GroupVerification claim(Owner owner, String groupId, VerificationMethod method) {
		log.info(
				"Claiming group verification for owner {} ({}), groupId '{}', method {}",
				owner.slug(),
				owner.id(),
				groupId,
				method
		);

		findAnyOverlapping(groupId).ifPresent(ignore -> {
			throw new GroupIdAlreadyClaimedException(groupId);
		});

		final GroupVerification verification = save(
				GroupVerification.builder()
						.owner(owner)
						.groupId(groupId)
						.state(VerificationState.PENDING)
						.createdAt(OffsetDateTime.now())
						.build()
		);

		VerificationChallenge verificationChallenge = createChallenge(verification, method);
		save(verificationChallenge);

		return verification;
	}

	@Override
	@Transactional(label = "group-verifications.verify")
	public GroupVerification verify(Owner owner, String groupId) {
		log.info("Verifying group verification for owner {} ({}), groupId '{}'", owner.slug(), owner.id(), groupId);

		final GroupVerification verification = findByGroupId(groupId, owner)
				.orElseThrow(() -> new VerificationChallengeNotFoundException(owner, groupId));

		final VerificationChallenge challenge = findActiveChallenge(verification)
				.orElseThrow(() -> new VerificationChallengeNotFoundException("No active challenge to verify for groupId " + groupId));

		final VerificationStrategy strategy = resolveStrategy(challenge.method());
		log.debug("Using verification strategy {} for verification {} and challenge {}", strategy.method(), verification.id(), challenge.id());

		final VerificationResult result = strategy.verify(verification, challenge);
		final VerificationChallenge verifiedChallenge = applyVerificationResult(challenge, result);
		save(verifiedChallenge);

		if (result instanceof VerificationResult.Success) {
			Assert.state(
					verification.state() == VerificationState.PENDING,
					"Can only activate a pending verification, but was " + verification.state()
			);
			GroupVerification activated = verification.toBuilder()
					.state(VerificationState.ACTIVE)
					.verifiedAt(OffsetDateTime.now())
					.build();
			log.info(
					"Activated group verification {} for owner {} ({}), groupId '{}'",
					activated.id(),
					activated.owner().slug(),
					activated.owner().id(),
					activated.groupId()
			);
			return save(activated);
		}

		log.info(
				"Verification {} for owner {} ({}) and groupId '{}' failed with reason {}",
				verification.id(),
				owner.slug(),
				owner.id(),
				groupId,
				result
		);
		return verification;
	}

	@Override
	@Transactional(label = "group-verifications.revoke")
	public GroupVerification revoke(GroupVerification verification) {
		log.info(
				"Revoking group verification {} for owner {} ({}), groupId '{}', current state {}",
				verification.id(),
				verification.owner().slug(),
				verification.owner().id(),
				verification.groupId(),
				verification.state()
		);

		Assert.state(
				verification.state() == VerificationState.ACTIVE || verification.state() == VerificationState.PENDING,
				"Cannot revoke a \"" + verification.state() + "\" verification"
		);
		GroupVerification revoked = verification.toBuilder()
				.state(VerificationState.REVOKED)
				.revokedAt(OffsetDateTime.now())
				.build();
		final GroupVerification saved = save(revoked);
		log.info(
				"Revoked group verification {} for owner {} ({}), groupId '{}'",
				saved.id(),
				saved.owner().slug(),
				saved.owner().id(),
				saved.groupId()
		);
		return saved;
	}

	@Override
	@Transactional(readOnly = true, label = "group-verifications.find-owner")
	public Optional<Owner> findOwner(String namespace) {
		log.debug("Resolving owner for namespace slug '{}'", namespace);
		return context.select(NAMESPACES.ID, NAMESPACES.SLUG)
				.from(NAMESPACES)
				.where(NAMESPACES.SLUG.eq(namespace))
				.fetchOptional(record -> Owner.of(
						EntityId.from(record.get(NAMESPACES.ID)),
						record.get(NAMESPACES.SLUG)
				));
	}

	private GroupVerification save(GroupVerification verification) {
		log.debug(
				"Saving group verification for owner {} ({}), groupId '{}', state {}",
				verification.owner().slug(),
				verification.owner().id(),
				verification.groupId(),
				verification.state()
		);

		final Record record = context.insertInto(GROUP_VERIFICATIONS)
				.set(SettableRecord.of(context, GROUP_VERIFICATIONS)
						.set(GROUP_VERIFICATIONS.NAMESPACE_ID, verification.owner().id().get())
						.set(GROUP_VERIFICATIONS.GROUP_ID, verification.groupId())
						.set(GROUP_VERIFICATIONS.STATE, verification.state().name())
						.set(GROUP_VERIFICATIONS.CREATED_AT, verification.createdAt())
						.set(GROUP_VERIFICATIONS.VERIFIED_AT, verification.verifiedAt())
						.set(GROUP_VERIFICATIONS.REVOKED_AT, verification.revokedAt())
						.get())
				.onConflict(GROUP_VERIFICATIONS.NAMESPACE_ID, GROUP_VERIFICATIONS.GROUP_ID)
				.doUpdate()
				.set(GROUP_VERIFICATIONS.STATE, verification.state().name())
				.set(GROUP_VERIFICATIONS.VERIFIED_AT, verification.verifiedAt())
				.set(GROUP_VERIFICATIONS.REVOKED_AT, verification.revokedAt())
				.returning(GROUP_VERIFICATIONS.fields())
				.fetchOne();

		Assert.state(record != null, "Could not save group verification: " + verification.groupId());
		return toGroupVerification(record, verification.owner());
	}

	private VerificationChallenge save(VerificationChallenge challenge) {
		Assert.notNull(challenge.verificationId(), "Verification challenge must be attached to a verification before saving");

		log.debug(
				"Saving challenge for verificationId {}, method {}, state {}, challengeId {}",
				challenge.verificationId(),
				challenge.method(),
				challenge.state(),
				challenge.id()
		);

		var insert = context.insertInto(GROUP_VERIFICATION_CHALLENGES);
		if (challenge.id() != null) {
			insert.set(GROUP_VERIFICATION_CHALLENGES.ID, challenge.id().get());
		}

		final Record record = insert
				.set(SettableRecord.of(context, GROUP_VERIFICATION_CHALLENGES)
						.set(GROUP_VERIFICATION_CHALLENGES.GROUP_VERIFICATION_ID, challenge.verificationId().get())
						.set(GROUP_VERIFICATION_CHALLENGES.VERIFICATION_METHOD, challenge.method().name())
						.set(GROUP_VERIFICATION_CHALLENGES.CHALLENGE_TOKEN, challenge.token())
						.set(GROUP_VERIFICATION_CHALLENGES.STATE, challenge.state().name())
						.set(GROUP_VERIFICATION_CHALLENGES.CREATED_AT, challenge.createdAt())
						.set(GROUP_VERIFICATION_CHALLENGES.VERIFIED_AT, challenge.verifiedAt())
						.set(GROUP_VERIFICATION_CHALLENGES.EXPIRES_AT, challenge.expiresAt())
						.get())
				.onConflict(GROUP_VERIFICATION_CHALLENGES.ID)
				.doUpdate()
				.set(GROUP_VERIFICATION_CHALLENGES.STATE, challenge.state().name())
				.set(GROUP_VERIFICATION_CHALLENGES.VERIFIED_AT, challenge.verifiedAt())
				.set(GROUP_VERIFICATION_CHALLENGES.EXPIRES_AT, challenge.expiresAt())
				.returning(GROUP_VERIFICATION_CHALLENGES.fields())
				.fetchOne();

		Assert.state(record != null, "Could not save verification challenge: " + challenge.id());
		return toVerificationChallenge(record);
	}

	private static Condition prefixOf(String groupId) {
		return DSL.val(groupId)
				.like(DSL.concat(GROUP_VERIFICATIONS.GROUP_ID, DSL.val(".%")))
				.or(GROUP_VERIFICATIONS.GROUP_ID.eq(groupId));
	}

	private static Condition overlaps(String groupId) {
		return prefixOf(groupId)
				.or(GROUP_VERIFICATIONS.GROUP_ID.like(DSL.concat(DSL.val(groupId), DSL.val(".%"))));
	}

	private static GroupVerification toGroupVerification(Record record) {
		return toGroupVerification(record,
				Owner.of(
						EntityId.from(record.get(GROUP_VERIFICATIONS.NAMESPACE_ID)),
						record.get(NAMESPACES.SLUG)
				));
	}

	private static GroupVerification toGroupVerification(Record record, Owner owner) {
		return GroupVerification.builder()
				.id(EntityId.from(record.get(GROUP_VERIFICATIONS.ID)))
				.owner(owner)
				.groupId(record.get(GROUP_VERIFICATIONS.GROUP_ID))
				.state(VerificationState.valueOf(record.get(GROUP_VERIFICATIONS.STATE)))
				.createdAt(record.get(GROUP_VERIFICATIONS.CREATED_AT))
				.verifiedAt(record.get(GROUP_VERIFICATIONS.VERIFIED_AT))
				.revokedAt(record.get(GROUP_VERIFICATIONS.REVOKED_AT))
				.build();
	}

	private static VerificationChallenge toVerificationChallenge(Record record) {
		return VerificationChallenge.builder()
				.id(EntityId.from(record.get(GROUP_VERIFICATION_CHALLENGES.ID)))
				.verificationId(EntityId.from(record.get(GROUP_VERIFICATION_CHALLENGES.GROUP_VERIFICATION_ID)))
				.method(VerificationMethod.valueOf(record.get(GROUP_VERIFICATION_CHALLENGES.VERIFICATION_METHOD)))
				.token(record.get(GROUP_VERIFICATION_CHALLENGES.CHALLENGE_TOKEN))
				.state(ChallengeState.valueOf(record.get(GROUP_VERIFICATION_CHALLENGES.STATE)))
				.createdAt(record.get(GROUP_VERIFICATION_CHALLENGES.CREATED_AT))
				.verifiedAt(record.get(GROUP_VERIFICATION_CHALLENGES.VERIFIED_AT))
				.expiresAt(record.get(GROUP_VERIFICATION_CHALLENGES.EXPIRES_AT))
				.build();
	}

	private VerificationStrategy resolveStrategy(VerificationMethod model) {
		return switch (model) {
			case DNS -> dnsTxtVerificationStrategy;
			case GITHUB -> throw new IllegalArgumentException("Not implemented");
		};
	}

	private VerificationChallenge applyVerificationResult(VerificationChallenge challenge, VerificationResult result) {
		Assert.state(
				challenge.state() == ChallengeState.UNVERIFIED,
				"Challenge must be " + ChallengeState.UNVERIFIED + " before it can be applied"
		);

		if (result instanceof VerificationResult.Success success) {
			Assert.state(
					success.method() == challenge.method(),
					"Cannot apply a " + success.method() + " result to a " + challenge.method() + " challenge"
			);

			return challenge.toBuilder()
					.state(ChallengeState.VERIFIED)
					.verifiedAt(OffsetDateTime.now())
					.build();
		}

		return challenge.toBuilder()
				.state(ChallengeState.UNVERIFIED)
				.verifiedAt(null)
				.build();
	}

	private VerificationChallenge createChallenge(GroupVerification verification, VerificationMethod method) {
		final byte[] seed = new byte[20];
		RANDOM.nextBytes(seed);

		return VerificationChallenge.builder()
				.method(method)
				.token(Base64.getUrlEncoder().withoutPadding().encodeToString(seed))
				.state(ChallengeState.UNVERIFIED)
				.createdAt(OffsetDateTime.now())
				.verificationId(verification.id())
				.build();
	}
}
