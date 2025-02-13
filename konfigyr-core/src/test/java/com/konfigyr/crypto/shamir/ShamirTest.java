package com.konfigyr.crypto.shamir;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ShamirTest {

	final SecureRandom random = new SecureRandom();

	@Test
	@DisplayName("should assert the minimum number of parts to recover the secret")
	void assertMinParts() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Shamir(1, 1))
				.withMessageContaining("Number of shares needed to recover the secret must be greater than 1");
	}

	@Test
	@DisplayName("should assert the minimum number of shares")
	void assertMinShares() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Shamir(2, 2))
				.withMessageContaining("Number of generated shares needs to greater than 2");
	}

	@Test
	@DisplayName("should assert the maximum number of shares")
	void assertMaxShares() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Shamir(2000, 3))
				.withMessageContaining("Number of generated shares can not be greater than 255");
	}

	@Test
	@DisplayName("should fail to generate secret for insecure secrets")
	void assertMinSecretLength() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Shamir(10, 3).generate(16))
				.withMessageContaining("Your secret must be at least 32 bytes or 128 bits long");
	}

	@Test
	@DisplayName("should generate shares from a 128 bit random secret")
	void generateDefaultRandomSecret() {
		final var shamir = new Shamir(random, 20, 5);
		final var shares = shamir.generate();

		assertThat(shares)
				.isNotEmpty()
				.hasSize(20)
				.isSortedAccordingTo(Share::compareTo);

		assertThat(shamir.join(randomize(shares, 5)).array())
				.hasSizeGreaterThanOrEqualTo(32);
	}

	@Test
	@DisplayName("should generate shares from a 2048 bit random secret")
	void generateSpecificRandomSecret() {
		final var shamir = new Shamir(random, 10, 3);
		final var shares = shamir.generate(256);

		assertThat(shares)
				.isNotEmpty()
				.hasSize(10)
				.isSortedAccordingTo(Share::compareTo);

		assertThat(shamir.join(randomize(shares, 3)).array())
				.hasSizeGreaterThanOrEqualTo(256);
	}

	@Test
	@DisplayName("should split secret in shares and recover it using secret key")
	void generateSharesAndRecoverSecret() {
		final var secret = "some secret value to be recovered";
		final var shamir = new Shamir(random, 15, 3);
		final var shares = shamir.split(secret.getBytes(StandardCharsets.UTF_8));

		assertThat(shares)
				.isNotEmpty()
				.hasSize(15)
				.isSortedAccordingTo(Share::compareTo);

		assertThat(shamir.join(randomize(shares, 3)).array())
				.asString(StandardCharsets.UTF_8)
				.isEqualTo(secret);
	}

	@Test
	@DisplayName("should fail to join null shares")
	void joinNullShares() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Shamir(random, 5, 3).join(null))
				.withMessageContaining("Shares list must not be null");
	}

	@Test
	@DisplayName("should fail to join empty shares")
	void joinEmptyShares() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Shamir(random, 5, 3).join(Collections.emptyList()))
				.withMessageContaining("You must provide at least 3 shares to recover the secret");
	}

	@Test
	@DisplayName("should fail to join with insufficient number of shares")
	void joinInsufficientShares() {
		final var shares = List.of(new Share(1, "share".getBytes(StandardCharsets.UTF_8)));

		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Shamir(random, 5, 2).join(shares))
				.withMessageContaining("You must provide at least 2 shares to recover the secret");
	}

	@Test
	@DisplayName("should fail to join with shares that have different/varying lengths")
	void joinSharesWithVaryingLengths() {
		final var shares = List.of(
				new Share(1, "first".getBytes(StandardCharsets.UTF_8)),
				new Share(2, "second".getBytes(StandardCharsets.UTF_8))
		);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Shamir(random, 5, 2).join(shares))
				.withMessageContaining("Varying lengths of shared values");
	}

	@Test
	@DisplayName("should fail to join with invalid shares")
	void joinInvalidShares() {
		final var secret = "some secret value to be recovered";
		final var shamir = new Shamir(random, 5, 2);
		final var shares = shamir.split(secret.getBytes(StandardCharsets.UTF_8));

		assertThat(shares)
				.isNotEmpty()
				.hasSize(5)
				.isSortedAccordingTo(Share::compareTo);

		final var invalid = List.of(
				new Share(1, shares.getLast().value()),
				new Share(2, shares.getFirst().value())
		);

		assertThat(shamir.join(invalid).array())
				.asString(StandardCharsets.UTF_8)
				.isNotEqualTo(secret);
	}

	private static List<Share> randomize(Collection<Share> shares, int length) {
		final var randomized = new ArrayList<>(shares);
		Collections.shuffle(randomized);

		return randomized.subList(0, length);
	}

}
