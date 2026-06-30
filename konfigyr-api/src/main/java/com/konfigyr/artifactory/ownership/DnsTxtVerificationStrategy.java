package com.konfigyr.artifactory.ownership;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.util.Assert;

import javax.naming.*;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/**
 * {@link VerificationStrategy} that proves ownership of a groupId by resolving a DNS {@code TXT}
 * record on the domain derived from it.
 * <p>
 * The lookup domain is built by reversing the first two components of the groupId, so
 * {@code com.konfigyr.app} is verified against {@code konfigyr.com}. Ownership is confirmed when one
 * of the domain's {@code TXT} records exactly matches {@code konfigyr-verification=<challenge token>}.
 * <p>
 * Lookup outcomes are mapped to {@link VerificationResult.FailureReason failure reasons} as follows:
 * <ul>
 *     <li>{@link VerificationResult.FailureReason#TARGET_NOT_FOUND TARGET_NOT_FOUND} – the domain has
 *     no {@code TXT} records or does not exist;</li>
 *     <li>{@link VerificationResult.FailureReason#TOKEN_MISMATCH TOKEN_MISMATCH} – {@code TXT} records
 *     exist but none carry the expected token;</li>
 *     <li>{@link VerificationResult.FailureReason#SERVICE_UNAVAILABLE SERVICE_UNAVAILABLE} – the DNS
 *     lookup could not be completed due to a communication or service failure;</li>
 *     <li>{@link VerificationResult.FailureReason#INTERNAL_ERROR INTERNAL_ERROR} – any other naming
 *     failure.</li>
 * </ul>
 *
 * @author Mila Zarkovic
 * @since 1.0.0
 * @see VerificationStrategy
 * @see VerificationMethod#DNS
 */
@Slf4j
@NullMarked
class DnsTxtVerificationStrategy implements VerificationStrategy {
	private static final Marker MARKER = MarkerFactory.getMarker("DNS_VERIFIER");

	static final String TXT_RECORD_PREFIX = "konfigyr-verification=";
	static final int DNS_TIMEOUT_MS = 3000;
	static final int DNS_RETRIES = 1;

	@Override
	public VerificationMethod method() {
		return VerificationMethod.DNS;
	}

	@Override
	public VerificationResult verify(GroupVerification verification, VerificationChallenge challenge) {
		final String domain = toDomain(verification.groupId());

		try (CloseableDirContext ctx = new CloseableDirContext()) {
			final Attributes attrs = ctx.getAttributes("dns:/" + domain, new String[]{"TXT"});
			final Attribute txtAttr = attrs.get("TXT");

			if (txtAttr == null) {
				return VerificationResult.failure(VerificationResult.FailureReason.TARGET_NOT_FOUND);
			}

			final String expected = TXT_RECORD_PREFIX + challenge.token();
			final NamingEnumeration<?> values = txtAttr.getAll();

			while (values.hasMore()) {
				if (expected.equals(values.next().toString())) {
					return VerificationResult.success(VerificationMethod.DNS);
				}
			}

			return VerificationResult.failure(VerificationResult.FailureReason.TOKEN_MISMATCH);
		} catch (NameNotFoundException e) {
			return VerificationResult.failure(VerificationResult.FailureReason.TARGET_NOT_FOUND);
		} catch (CommunicationException | ServiceUnavailableException e) {
			log.warn(MARKER, "DNS lookup failed for domain {} due communication failure", domain, e);
			return VerificationResult.failure(VerificationResult.FailureReason.SERVICE_UNAVAILABLE);
		} catch (NamingException e) {
			log.error(MARKER, "DNS lookup failed for domain {}", domain, e);
			return VerificationResult.failure(VerificationResult.FailureReason.INTERNAL_ERROR);
		}
	}

	private static String toDomain(String groupId) {
		final String[] parts = groupId.split("\\.");
		Assert.isTrue(parts.length >= 2, "Group must contain at least two dot-separated components: " + groupId);
		return parts[1] + "." + parts[0];
	}

	private static final class CloseableDirContext implements AutoCloseable {

		private final InitialDirContext ctx;

		CloseableDirContext() throws NamingException {
			final Hashtable<String, String> env = new Hashtable<>();
			env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
			env.put("com.sun.jndi.dns.timeout.initial", String.valueOf(DNS_TIMEOUT_MS));
			env.put("com.sun.jndi.dns.timeout.retries", String.valueOf(DNS_RETRIES));
			this.ctx = new InitialDirContext(env);
		}

		Attributes getAttributes(String name, String[] attrIds) throws NamingException {
			return ctx.getAttributes(name, attrIds);
		}

		@Override
		public void close() throws NamingException {
			ctx.close();
		}
	}
}
