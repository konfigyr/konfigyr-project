package com.konfigyr.artifactory.ownership;

import com.konfigyr.artifactory.Owner;
import com.konfigyr.data.PageableExecutor;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.GroupVerificationChallenges.GROUP_VERIFICATION_CHALLENGES;
import static com.konfigyr.data.tables.GroupVerifications.GROUP_VERIFICATIONS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

@Slf4j
@NullMarked
@RequiredArgsConstructor
class DefaultGroupVerifications implements GroupVerifications {

	private static final PageableExecutor groupVerificationPageableExecutor = PageableExecutor.builder()
			.defaultSortField(GROUP_VERIFICATIONS.CREATED_AT.desc())
			.sortField("date", GROUP_VERIFICATIONS.CREATED_AT)
			.sortField("group", GROUP_VERIFICATIONS.GROUP_ID)
			.build();

	private final BytesKeyGenerator challengeTokenGenerator = KeyGenerators.secureRandom(6);

	private final DSLContext context;
	private final VerificationStrategies strategies;

	@Override
	@Transactional(readOnly = true, label = "group-verifications.find-active-covering")
	public Optional<GroupVerification> findActiveCovering(Owner owner, String groupId) {
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
	public Page<GroupVerification> findByOwner(Owner owner, SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();
		conditions.add(GROUP_VERIFICATIONS.NAMESPACE_ID.eq(owner.id().get()));

		query.term().ifPresent(term -> conditions.add(
				GROUP_VERIFICATIONS.GROUP_ID.containsIgnoreCase(term)
		));

		query.criteria(GroupVerification.STATE_CRITERIA).ifPresent(state -> conditions.add(
				GROUP_VERIFICATIONS.STATE.eq(state.name())
		));

		return groupVerificationPageableExecutor.execute(
				createGroupVerificationsQuery(DSL.and(conditions)),
				DefaultGroupVerifications::toGroupVerification,
				query.pageable(),
				() -> context.fetchCount(createGroupVerificationsQuery(DSL.and(conditions)))
		);
	}

	@Override
	@Transactional(readOnly = true, label = "group-verifications.find-by-group-id")
	public Optional<GroupVerification> findByGroupId(Owner owner, String groupId) {
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
		return context.select(GROUP_VERIFICATION_CHALLENGES.fields())
				.from(GROUP_VERIFICATION_CHALLENGES)
				.where(GROUP_VERIFICATION_CHALLENGES.GROUP_VERIFICATION_ID.eq(verification.id().get()))
				.and(GROUP_VERIFICATION_CHALLENGES.STATE.eq(ChallengeState.UNVERIFIED.name()))
				.fetchOptional(DefaultGroupVerifications::toVerificationChallenge);
	}

	@Override
	@Transactional(readOnly = true, label = "group-verifications.find-challenges")
	public List<VerificationChallenge> findChallenges(GroupVerification verification) {
		return context.select(GROUP_VERIFICATION_CHALLENGES.fields())
				.from(GROUP_VERIFICATION_CHALLENGES)
				.where(GROUP_VERIFICATION_CHALLENGES.GROUP_VERIFICATION_ID.eq(verification.id().get()))
				.orderBy(GROUP_VERIFICATION_CHALLENGES.CREATED_AT.asc())
				.fetch(DefaultGroupVerifications::toVerificationChallenge);
	}

	@Override
	@Transactional(label = "group-verifications.claim")
	public GroupVerification claim(Owner owner, String groupId, VerificationMethod method) {
		log.debug("Attempting to create a group verification and claim challenge for: [owner={}, groupId={}, method={}]",
				owner, groupId, method);

		findAnyOverlapping(groupId).ifPresent(ignore -> {
			throw new GroupIdAlreadyClaimedException(groupId);
		});

		final GroupVerification verification = context.insertInto(GROUP_VERIFICATIONS)
				.set(SettableRecord.of(context, GROUP_VERIFICATIONS)
						.set(GROUP_VERIFICATIONS.ID, EntityId.generate().map(EntityId::get))
						.set(GROUP_VERIFICATIONS.NAMESPACE_ID, owner.id().get())
						.set(GROUP_VERIFICATIONS.GROUP_ID, groupId)
						.set(GROUP_VERIFICATIONS.STATE, VerificationState.PENDING.name())
						.set(GROUP_VERIFICATIONS.CREATED_AT, OffsetDateTime.now())
						.get())
				.returning(GROUP_VERIFICATIONS.fields())
				.fetchOne(it -> toGroupVerification(it, owner));

		Assert.state(verification != null, () -> "Could not create verification for: [owner=%s, groupId=%s]"
				.formatted(owner.slug(),  groupId));

		final VerificationChallenge challenge = context.insertInto(GROUP_VERIFICATION_CHALLENGES)
				.set(GROUP_VERIFICATION_CHALLENGES.GROUP_VERIFICATION_ID, verification.id().get())
				.set(GROUP_VERIFICATION_CHALLENGES.VERIFICATION_METHOD, method.name())
				.set(GROUP_VERIFICATION_CHALLENGES.CHALLENGE_TOKEN, generateChallengeToken())
				.set(GROUP_VERIFICATION_CHALLENGES.STATE, ChallengeState.UNVERIFIED.name())
				.set(GROUP_VERIFICATION_CHALLENGES.CREATED_AT, verification.createdAt())
				.returning(GROUP_VERIFICATION_CHALLENGES.fields())
				.fetchOne(DefaultGroupVerifications::toVerificationChallenge);

		Assert.state(challenge != null, () -> "Could not create challenge for: [verification=%s, groupId=%s, method=%s]"
				.formatted(verification.id(), verification.groupId(), method.name()));

		log.info("Successfully created group verification claim for owner {} ({}), groupId '{}', method {}",
				owner.slug(), owner.id(), groupId, method);

		return verification;
	}

	@Override
	@Transactional(label = "group-verifications.verify")
	public GroupVerification verify(Owner owner, String groupId) {
		log.debug("Attempting to perform group verification for: [owner={}, groupId={}]", owner, groupId);

		final GroupVerification verification = findByGroupId(owner, groupId)
				.orElseThrow(() -> new VerificationChallengeNotFoundException(owner, groupId));

		Assert.state(
				verification.state().canTransitionTo(VerificationState.ACTIVE),
				() -> "Can only verify a pending groupId verification, but it was in %s state".formatted(verification.state())
		);

		final VerificationChallenge challenge = findActiveChallenge(verification)
				.orElseThrow(() -> new VerificationChallengeNotFoundException("No active challenge to verify for groupId " + groupId));

		Assert.state(
				challenge.state() == ChallengeState.UNVERIFIED,
				() -> "Verification challenge must be in an '%s' state before it can be applied, but it was in %s state"
						.formatted(ChallengeState.UNVERIFIED, challenge.state())
		);

		final VerificationStrategy strategy = strategies.get(challenge.method());
		log.debug("Using verification strategy {} for verification {} and challenge {}",
				strategy.method(), verification.id(), challenge.id());

		final VerificationResult result = strategy.verify(verification, challenge);

		if (result instanceof VerificationResult.Success(VerificationMethod method)) {
			Assert.state(method == challenge.method(), "Verification methods do not match");

			final GroupVerification activated = verification.toBuilder()
					.state(VerificationState.ACTIVE)
					.verifiedAt(OffsetDateTime.now())
					.build();

			context.update(GROUP_VERIFICATIONS)
					.set(GROUP_VERIFICATIONS.STATE, activated.state().name())
					.set(GROUP_VERIFICATIONS.VERIFIED_AT, activated.verifiedAt())
					.where(GROUP_VERIFICATIONS.ID.eq(activated.id().get()))
					.execute();

			context.update(GROUP_VERIFICATION_CHALLENGES)
					.set(GROUP_VERIFICATION_CHALLENGES.STATE, ChallengeState.VERIFIED.name())
					.set(GROUP_VERIFICATION_CHALLENGES.VERIFIED_AT, activated.verifiedAt())
					.where(GROUP_VERIFICATION_CHALLENGES.ID.eq(challenge.id()))
					.execute();

			log.info(
					"Activated group verification {} for owner {} ({}), groupId '{}'",
					activated.id(),
					activated.owner().slug(),
					activated.owner().id(),
					activated.groupId()
			);

			return activated;
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
		log.debug("Attempting to revoke a group verification for: [owner={}, groupId={}, state={}]",
				verification.owner(), verification.groupId(), verification.state()
		);

		Assert.state(
				verification.state().canTransitionTo(VerificationState.REVOKED),
				() -> "Can only revoke an active groupId verification, but it was in a '%s' state".formatted(verification.state())
		);

		final GroupVerification revoked = verification.toBuilder()
				.state(VerificationState.REVOKED)
				.revokedAt(OffsetDateTime.now())
				.build();

		context.update(GROUP_VERIFICATIONS)
				.set(GROUP_VERIFICATIONS.STATE, revoked.state().name())
				.set(GROUP_VERIFICATIONS.REVOKED_AT, revoked.revokedAt())
				.where(GROUP_VERIFICATIONS.ID.eq(revoked.id().get()))
				.execute();

		log.info(
				"Successfully revoked group verification {} for owner {} ({}), groupId '{}'",
				revoked.id(),
				revoked.owner().slug(),
				revoked.owner().id(),
				revoked.groupId()
		);

		return revoked;
	}

	private String generateChallengeToken() {
		final byte[] seed = challengeTokenGenerator.generateKey();
		return Hex.encodeHexString(seed);
	}

	private SelectConditionStep<? extends Record> createGroupVerificationsQuery(Condition condition) {
		return context.select(GROUP_VERIFICATIONS.fields())
				.select(NAMESPACES.SLUG)
				.from(GROUP_VERIFICATIONS)
				.join(NAMESPACES).on(GROUP_VERIFICATIONS.NAMESPACE_ID.eq(NAMESPACES.ID))
				.where(condition);
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
		return toGroupVerification(record, new Owner(
				record.get(GROUP_VERIFICATIONS.NAMESPACE_ID, EntityId.class),
				record.get(NAMESPACES.SLUG)
		));
	}

	private static GroupVerification toGroupVerification(Record record, Owner owner) {
		return GroupVerification.builder()
				.id(record.get(GROUP_VERIFICATIONS.ID, EntityId.class))
				.owner(owner)
				.groupId(record.get(GROUP_VERIFICATIONS.GROUP_ID))
				.state(record.get(GROUP_VERIFICATIONS.STATE, VerificationState.class))
				.createdAt(record.get(GROUP_VERIFICATIONS.CREATED_AT))
				.verifiedAt(record.get(GROUP_VERIFICATIONS.VERIFIED_AT))
				.revokedAt(record.get(GROUP_VERIFICATIONS.REVOKED_AT))
				.build();
	}

	private static VerificationChallenge toVerificationChallenge(Record record) {
		return VerificationChallenge.builder()
				.id(record.get(GROUP_VERIFICATION_CHALLENGES.ID))
				.verificationId(record.get(GROUP_VERIFICATION_CHALLENGES.GROUP_VERIFICATION_ID, EntityId.class))
				.method(record.get(GROUP_VERIFICATION_CHALLENGES.VERIFICATION_METHOD, VerificationMethod.class))
				.token(record.get(GROUP_VERIFICATION_CHALLENGES.CHALLENGE_TOKEN))
				.state(record.get(GROUP_VERIFICATION_CHALLENGES.STATE, ChallengeState.class))
				.createdAt(record.get(GROUP_VERIFICATION_CHALLENGES.CREATED_AT))
				.verifiedAt(record.get(GROUP_VERIFICATION_CHALLENGES.VERIFIED_AT))
				.expiresAt(record.get(GROUP_VERIFICATION_CHALLENGES.EXPIRES_AT))
				.build();
	}

}
