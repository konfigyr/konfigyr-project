package com.konfigyr.identity.authorization.jwk;

import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.data.converter.EncryptionConverter;
import com.konfigyr.data.tables.OauthKeys;
import com.konfigyr.io.ByteArray;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.*;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Record;
import org.jooq.*;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.function.ThrowingFunction;

import java.security.Key;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static com.konfigyr.data.tables.OauthKeys.OAUTH_KEYS;

@Slf4j
public class KeyRepository {

	private final Converter<String, KeyAlgorithm> algorithmConverter = Converter.of(
			String.class, KeyAlgorithm.class, KeyAlgorithm::valueOf, KeyAlgorithm::name
	);
	private final Converter<OffsetDateTime, Date> dateConverter = Converter.of(
			OffsetDateTime.class, Date.class,
			date -> Date.from(date.toInstant()),
			date -> OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC)
	);

	private final DSLContext context;
	private final Converter<ByteArray, ByteArray> encryptionConverter;

	public KeyRepository(DSLContext context, KeysetOperations operations) {
		this.context = context;
		this.encryptionConverter = EncryptionConverter.create(operations);
	}

	@NonNull
	@Transactional(readOnly = true, label = "oauth-key-repository.get")
	public List<JWK> get() {
		return createQuery().fetch(this::convert);
	}

	@NonNull
	@Transactional(readOnly = true, label = "oauth-key-repository.get-by-id")
	public Optional<JWK> get(@NonNull String id) {
		return createQuery(OAUTH_KEYS.ID.eq(id)).fetchOptional(this::convert);
	}

	@NonNull
	@Transactional(label = "oauth-key-repository.create")
	public <T extends JWK & AsymmetricJWK> T create(@NonNull KeyAlgorithm algorithm, @NonNull Period expiration) {
		final T key;

		if (log.isDebugEnabled()) {
			log.debug("Creating new JWK using [algorithm={}, expiration={}]", algorithm, expiration);
		}

		try {
			key = algorithm.generate(expiration);
		} catch (JOSEException ex) {
			throw new IllegalStateException("failed to create JWK", ex);
		}

		context.insertInto(OAUTH_KEYS)
				.set(OAUTH_KEYS.ID, key.getKeyID())
				.set(OAUTH_KEYS.KEY_ALGORITHM.convert(algorithmConverter), algorithm)
				.set(OAUTH_KEYS.PUBLIC_KEY, encode(key, AsymmetricJWK::toPublicKey, UnaryOperator.identity()))
				.set(OAUTH_KEYS.PRIVATE_KEY, encode(key, AsymmetricJWK::toPrivateKey, encryptionConverter::to))
				.set(OAUTH_KEYS.ISSUED_AT.convert(dateConverter), key.getIssueTime())
				.set(OAUTH_KEYS.EXPIRES_AT.convert(dateConverter), key.getExpirationTime())
				.execute();

		log.info("Successfully created new JWK: [id={}, algorithm={}, issued_at={}, expires_at={}]",
				key.getKeyID(), key.getAlgorithm(), key.getIssueTime(), key.getExpirationTime());

		return key;
	}

	@Transactional(label = "oauth-key-repository.delete")
	public void delete(@NonNull String id) {
		final long rows = context.deleteFrom(OAUTH_KEYS)
				.where(OAUTH_KEYS.ID.eq(id))
				.execute();

		if (rows == 1) {
			log.info("Successfully removed JWK with identifier: {}", id);
		}
	}

	@NonNull
	private ResultQuery<Record> createQuery(Condition... conditions) {
		return context.select(OauthKeys.OAUTH_KEYS.fields())
				.from(OAUTH_KEYS)
				.where(conditions)
				.orderBy(OAUTH_KEYS.EXPIRES_AT.desc());
	}

	/**
	 * Creates an {@link JWK Asymmetric JWK} based on the retrieved jOOQ record.
	 *
	 * @param record retrieved record, can't be {@literal null}
	 * @return the Asymmetric JWK, never {@literal null}
	 */
	private JWK convert(@NonNull Record record) {
		final KeyAlgorithm algorithm = record.get(OAUTH_KEYS.KEY_ALGORITHM, algorithmConverter);

		final KeyPair pair;

		try {
			pair = algorithm.createKeyPair(
					record.get(OAUTH_KEYS.PUBLIC_KEY),
					record.get(OAUTH_KEYS.PRIVATE_KEY, encryptionConverter)
			);
		} catch (JOSEException ex) {
			throw new IllegalStateException("failed to create key pair", ex);
		}

		return switch (algorithm) {
			case RS256, RSA_OAEP_256 -> new RSAKey.Builder((RSAPublicKey) pair.getPublic())
					.keyID(record.get(OAUTH_KEYS.ID))
					.keyUse(algorithm.usage())
					.algorithm(algorithm.get())
					.keyOperations(algorithm.operations())
					.privateKey((RSAPrivateKey) pair.getPrivate())
					.issueTime(record.get(OAUTH_KEYS.ISSUED_AT, dateConverter))
					.notBeforeTime(record.get(OAUTH_KEYS.ISSUED_AT, dateConverter))
					.expirationTime(record.get(OAUTH_KEYS.EXPIRES_AT, dateConverter))
					.build();
			case ES256, ECDH_ES_A128KW -> new ECKey.Builder(Curve.P_256, (ECPublicKey) pair.getPublic())
					.keyID(record.get(OAUTH_KEYS.ID))
					.keyUse(algorithm.usage())
					.algorithm(algorithm.get())
					.keyOperations(algorithm.operations())
					.privateKey((ECPrivateKey) pair.getPrivate())
					.issueTime(record.get(OAUTH_KEYS.ISSUED_AT, dateConverter))
					.notBeforeTime(record.get(OAUTH_KEYS.ISSUED_AT, dateConverter))
					.expirationTime(record.get(OAUTH_KEYS.EXPIRES_AT, dateConverter))
					.build();
		};
	}

	private static <T extends Key> ByteArray encode(AsymmetricJWK key, ThrowingFunction<AsymmetricJWK, T> extractor, UnaryOperator<ByteArray> converter) {
		final T value = extractor.apply(key, InternalAuthenticationServiceException::new);
		final ByteArray encoded = new ByteArray(value.getEncoded());

		return converter.apply(encoded);
	}

}
