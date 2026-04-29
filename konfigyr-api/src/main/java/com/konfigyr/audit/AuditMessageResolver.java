package com.konfigyr.audit;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;

import java.util.Map;

/**
 * Resolves human-readable messages for {@link AuditRecord audit records} using Spring's
 * {@link MessageSource} mechanism.
 * <p>
 * Messages are looked up by the event type using the key pattern {@code audit.event.<eventType>},
 * where the dot-separated event type is used directly (e.g. {@code audit.event.namespace.renamed}).
 * Event detail values are passed as indexed message arguments in alphabetical key order, allowing
 * message templates to reference them via {@code {0}}, {@code {1}}, etc.
 * <p>
 * When no message is found for a given event type, the raw event type string is returned as a
 * fallback to ensure every audit record always has a displayable message.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see AuditRecord
 */
@NullMarked
final class AuditMessageResolver {

	private static final String MESSAGE_KEY_PREFIX = "audit.event.";

	private final MessageSourceAccessor messageSourceAccessor;

	AuditMessageResolver(MessageSource messageSource) {
		this.messageSourceAccessor = new MessageSourceAccessor(messageSource);
	}

	/**
	 * Resolves a human-readable message for the given audit event type and details.
	 *
	 * @param eventType the event type string, must not be {@literal null}
	 * @param details optional event-specific payload, can be {@literal null}
	 * @return the resolved message, never {@literal null}
	 */
	String resolve(String eventType, @Nullable Map<String, Object> details) {
		final String code = MESSAGE_KEY_PREFIX + eventType;
		final Object[] args = extractArguments(details);

		return messageSourceAccessor.getMessage(code, args, eventType);
	}

	private static Object[] extractArguments(@Nullable Map<String, Object> details) {
		if (details == null || details.isEmpty()) {
			return new Object[0];
		}

		return details.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(Map.Entry::getValue)
				.toArray();
	}

}
