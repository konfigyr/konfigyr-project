package com.konfigyr.identity.authorization.issuer;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.function.SingletonSupplier;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link TrustedIssuerRepository} backed by a static map of well-known public OIDC
 * identity providers whose issuer URIs are fixed and globally shared across all tenants.
 * <p>
 * These are providers where every operator and every namespace can safely use the same
 * issuer URI without per-deployment configuration:
 * <ul>
 *     <li><b>GitHub Actions</b>: {@code https://token.actions.githubusercontent.com}</li>
 *     <li><b>GitLab</b>: {@code https://gitlab.com}</li>
 * </ul>
 * Self-hosted or per-tenant issuers (Jenkins, HashiCorp Vault, Spacelift, GitLab
 * self-managed, etc.) have instance-specific URIs and must be registered through a
 * namespace-scoped repository instead.
 * <p>
 * The single instance is obtained via {@link #getInstance()} and is thread-safe. No audience
 * validation is applied by default; audience constraints can be added at the namespace level
 * when a specific audience is required.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
final class WellKnownTrustedIssuers implements TrustedIssuerRepository {

	private static final Supplier<TrustedIssuerRepository> INSTANCE = SingletonSupplier.of(WellKnownTrustedIssuers::new);

	/**
	 * Returns the singleton instance of {@link WellKnownTrustedIssuers}.
	 *
	 * @return the singleton repository
	 */
	static TrustedIssuerRepository getInstance() {
		return INSTANCE.get();
	}

	private final Map<String, TrustedIssuerRegistration> issuers;

	private WellKnownTrustedIssuers() {
		this.issuers = Stream.of(
				TrustedIssuerRegistration.withId("github-actions")
						.name("GitHub Actions")
						.issuerUri("https://token.actions.githubusercontent.com")
						.build(),
				TrustedIssuerRegistration.withId("gitlab")
						.name("GitLab")
						.issuerUri("https://gitlab.com")
						.build()
		).collect(Collectors.toUnmodifiableMap(
				TrustedIssuerRegistration::issuerUri,
				Function.identity()
		));
	}

	/**
	 * Returns the well-known {@link TrustedIssuerRegistration} for the given issuer URI, or
	 * {@code null} if the URI does not match any of the built-in entries. The namespace
	 * parameter is ignored because these issuers are globally trusted regardless of
	 * which namespace is requesting the lookup.
	 */
	@Override
	public @Nullable TrustedIssuerRegistration lookup(EntityId namespace, String issuerUri) {
		return issuers.get(issuerUri);
	}

}
