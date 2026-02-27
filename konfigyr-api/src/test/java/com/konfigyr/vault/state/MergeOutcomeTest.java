package com.konfigyr.vault.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MergeOutcomeTest {

	static String NAME = "ref/profiles/test-profile";
	static String AUTHOR = "John Doe <john.doe@konfigyr.com>";

	@Test
	@DisplayName("should create unknown merge outcomes")
	void createUnknownMergeOutcome() {
		final var outcome = MergeOutcome.unknown(NAME, AUTHOR);

		assertThat(outcome)
				.returns(NAME, MergeOutcome::branch)
				.returns(null, MergeOutcome::revision)
				.returns(null, MergeOutcome::conflicts)
				.returns(true, MergeOutcome::isUnknown)
				.returns(false, MergeOutcome::isApplied)
				.returns(false, MergeOutcome::isConflicting);

		assertThat(outcome)
				.isEqualTo(MergeOutcome.unknown(NAME, AUTHOR))
				.hasSameHashCodeAs(MergeOutcome.unknown(NAME, AUTHOR));

		assertThat(outcome)
				.isNotEqualTo(MergeOutcome.unknown("main", AUTHOR))
				.isNotEqualTo(MergeOutcome.applied(NAME, AUTHOR, "commit-id"))
				.isNotEqualTo(MergeOutcome.conflicting(NAME, AUTHOR, "conflicts"));

		assertThat(outcome.toString())
				.isEqualTo("MergeOutcome(%s, outcome=unknown)", NAME);
	}

	@Test
	@DisplayName("should create applied merge outcomes")
	void createAppliedMergeOutcome() {
		final var outcome = MergeOutcome.applied(NAME, AUTHOR, "commit-id");

		assertThat(outcome)
				.returns(NAME, MergeOutcome::branch)
				.returns("commit-id", MergeOutcome::revision)
				.returns(null, MergeOutcome::conflicts)
				.returns(true, MergeOutcome::isApplied)
				.returns(false, MergeOutcome::isUnknown)
				.returns(false, MergeOutcome::isConflicting);

		assertThat(outcome)
				.isEqualTo(MergeOutcome.applied(NAME, AUTHOR, "commit-id"))
				.hasSameHashCodeAs(MergeOutcome.applied(NAME, AUTHOR, "commit-id"));

		assertThat(outcome)
				.isNotEqualTo(MergeOutcome.applied("main", AUTHOR, "commit-id"))
				.isNotEqualTo(MergeOutcome.applied(NAME, AUTHOR, "different-commit-id"))
				.isNotEqualTo(MergeOutcome.conflicting(NAME, AUTHOR, "conflicts"));

		assertThat(outcome.toString())
				.isEqualTo("MergeOutcome(%s, revision=commit-id)", NAME);
	}

	@Test
	@DisplayName("should create conflicting merge outcomes")
	void createConflictingMergeOutcome() {
		final var outcome = MergeOutcome.conflicting(NAME, AUTHOR, "conflicts");

		assertThat(outcome)
				.returns(NAME, MergeOutcome::branch)
				.returns(null, MergeOutcome::revision)
				.returns("conflicts", MergeOutcome::conflicts)
				.returns(false, MergeOutcome::isApplied)
				.returns(false, MergeOutcome::isUnknown)
				.returns(true, MergeOutcome::isConflicting);

		assertThat(outcome)
				.isEqualTo(MergeOutcome.conflicting(NAME, AUTHOR, "conflicts"))
				.hasSameHashCodeAs(MergeOutcome.conflicting(NAME, AUTHOR, "conflicts"));

		assertThat(outcome)
				.isNotEqualTo(MergeOutcome.conflicting("main", AUTHOR, "conflicts"))
				.isNotEqualTo(MergeOutcome.applied(NAME, AUTHOR, "commit-id"))
				.isNotEqualTo(MergeOutcome.conflicting(NAME, AUTHOR, "different conflicts"));

		assertThat(outcome.toString())
				.isEqualTo("MergeOutcome(%s, outcome=conflict)", NAME);
	}

}
