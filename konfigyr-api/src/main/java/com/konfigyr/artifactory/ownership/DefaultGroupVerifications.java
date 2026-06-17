package com.konfigyr.artifactory.ownership;

import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.GroupVerificationChallenges.GROUP_VERIFICATION_CHALLENGES;
import static com.konfigyr.data.tables.GroupVerifications.GROUP_VERIFICATIONS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

@NullMarked
@RequiredArgsConstructor
class DefaultGroupVerifications implements GroupVerifications {

    private final DSLContext context;

    @Override
    @Transactional(readOnly = true, label = "group-verifications.find-active-covering")
    public Optional<GroupVerification> findActiveCovering(String groupId, Owner owner) {
        return context.select(GROUP_VERIFICATIONS.fields())
                .select(NAMESPACES.SLUG)
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
    public List<GroupVerification> findByOwner(Owner owner) {
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
    @Transactional(label = "group-verifications.save")
    public GroupVerification save(GroupVerification verification) {
        try {
            var insert = context.insertInto(GROUP_VERIFICATIONS);
            if (verification.id() != null) {
                insert.set(GROUP_VERIFICATIONS.ID, verification.id().get());
            }

            final Record record = insert
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
        } catch (DataAccessException ex) {
            throw ex;
        }
    }

    @Override
    @Transactional(label = "group-verifications.save-challenge")
    public VerificationChallenge saveChallenge(VerificationChallenge challenge) {
        Assert.notNull(challenge.verificationId(), "Verification challenge must be attached to a verification before saving");

        try {
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
        } catch (DataAccessException ex) {
            throw ex;
        }
    }

    private static Condition prefixOf(String groupId) {
        return DSL.inline(groupId).like(DSL.concat(GROUP_VERIFICATIONS.GROUP_ID, DSL.inline(".%")))
                .or(GROUP_VERIFICATIONS.GROUP_ID.eq(groupId));
    }

    private static Condition overlaps(String groupId) {
        return prefixOf(groupId)
                .or(GROUP_VERIFICATIONS.GROUP_ID.like(DSL.concat(DSL.inline(groupId), DSL.inline(".%"))));
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

}
