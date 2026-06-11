package com.konfigyr.identity.authorization.issuer;

import com.konfigyr.entity.EntityId;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A {@link TrustedIssuerRepository} that delegates to an ordered list of repositories
 * and returns the first non-null result. Returns {@code null} if no delegate resolves
 * the given namespace and issuer URI combination.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see TrustedIssuerRepository
 */
@NullMarked
@RequiredArgsConstructor
final class CompositeTrustedIssuerRepository implements TrustedIssuerRepository {

	private final Iterable<TrustedIssuerRepository> repositories;

	/**
	 * Constructs a composite from a varargs array of repositories consulted in order.
	 *
	 * @param repositories ordered repositories to delegate to
	 */
	CompositeTrustedIssuerRepository(TrustedIssuerRepository... repositories) {
		this(List.of(repositories));
	}

	@Override
	public @Nullable TrustedIssuerRegistration lookup(EntityId namespace, String issuerUri) {
		for (TrustedIssuerRepository repository : repositories) {
			final TrustedIssuerRegistration issuer = repository.lookup(namespace, issuerUri);

			if (issuer != null) {
				return issuer;
			}
		}

		return null;
	}

}
