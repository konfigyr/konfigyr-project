package com.konfigyr.identity.authorization.issuer;

import com.konfigyr.data.converter.JsonbConverter;
import com.konfigyr.entity.EntityId;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Converter;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static com.konfigyr.data.tables.NamespaceTrustedIssuers.NAMESPACE_TRUSTED_ISSUERS;

/**
 * A {@link TrustedIssuerRepository} backed by the {@code namespace_trusted_issuers} database
 * table. Returns a {@link TrustedIssuerRegistration} only when an active entry exists for the
 * given namespace and issuer URI combination; returns {@code null} otherwise.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
@NullMarked
class NamespaceTrustedIssuerRepository implements TrustedIssuerRepository {

	private final DSLContext context;
	private final Converter<JSONB, List<String>> audiencesConverter;

	NamespaceTrustedIssuerRepository(DSLContext context, JsonMapper jsonMapper) {
		this.context = context;
		this.audiencesConverter = JsonbConverter.create(jsonMapper, jsonMapper.getTypeFactory()
				.constructCollectionType(List.class, String.class));
	}

	@Nullable
	@Override
	@Transactional(readOnly = true, label = "namespace-trusted-issuer-repository.lookup")
	public TrustedIssuerRegistration lookup(EntityId namespace, String issuerUri) {
		return context.select(
						NAMESPACE_TRUSTED_ISSUERS.ID,
						NAMESPACE_TRUSTED_ISSUERS.NAME,
						NAMESPACE_TRUSTED_ISSUERS.ISSUER_URI,
						NAMESPACE_TRUSTED_ISSUERS.JWKS_URI,
						NAMESPACE_TRUSTED_ISSUERS.ALLOWED_AUDIENCES
				)
				.from(NAMESPACE_TRUSTED_ISSUERS)
				.where(DSL.and(
						NAMESPACE_TRUSTED_ISSUERS.NAMESPACE_ID.eq(namespace.get()),
						NAMESPACE_TRUSTED_ISSUERS.ISSUER_URI.eq(issuerUri),
						NAMESPACE_TRUSTED_ISSUERS.IS_ACTIVE.isTrue()
				))
				.fetchOne(this::toRegistration);
	}

	private TrustedIssuerRegistration toRegistration(Record record) {
		final String id = EntityId.from(record.get(NAMESPACE_TRUSTED_ISSUERS.ID)).serialize();

		if (log.isDebugEnabled()) {
			log.debug("Resolved namespace trusted issuer: [id={}, issuer={}]",
					id, record.get(NAMESPACE_TRUSTED_ISSUERS.ISSUER_URI));
		}

		final List<String> audiences = record.get(NAMESPACE_TRUSTED_ISSUERS.ALLOWED_AUDIENCES, audiencesConverter);

		return TrustedIssuerRegistration.withId(id)
				.name(record.get(NAMESPACE_TRUSTED_ISSUERS.NAME))
				.issuerUri(record.get(NAMESPACE_TRUSTED_ISSUERS.ISSUER_URI))
				.jwksUri(record.get(NAMESPACE_TRUSTED_ISSUERS.JWKS_URI))
				.allowedAudiences(audiences)
				.build();
	}

}
