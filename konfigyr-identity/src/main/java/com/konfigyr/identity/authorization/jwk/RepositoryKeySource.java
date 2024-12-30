package com.konfigyr.identity.authorization.jwk;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class RepositoryKeySource implements JWKSource<SecurityContext>, InitializingBean {

	static final Period CRYPTO_PERIOD = Period.ofYears(1);
	static final Duration EXPIRATION_SKEW = Duration.ofMinutes(60);

	private final KeyRepository repository;
	private JWKSet keys = null;

	@Override
	public void afterPropertiesSet() {
		keys = load();
	}

	@Override
	public List<JWK> get(@NonNull JWKSelector selector, @Nullable SecurityContext ctx) throws KeySourceException {
		Assert.state(keys != null, "JWK source has not been initialized");

		return selector.select(keys);
	}

	@Scheduled(cron = "0 0 * * * *")
	void reload() {
		this.keys = load();
	}

	@NonNull
	private JWKSet load() {
		final List<JWK> keys = new ArrayList<>();

		if (log.isDebugEnabled()) {
			log.debug("Loading JWKs from repository...");
		}

		final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

		for (JWK key : repository.get()) {
			log.info("Checking if JWK should be removed [id={}, algorithm={}, issued_at={}, expires_at={}]",
					key.getKeyID(), key.getAlgorithm(), key.getIssueTime(), key.getExpirationTime());

			final OffsetDateTime expirationTime = toExpirationTime(key.getExpirationTime());

			if (expirationTime != null && expirationTime.minus(EXPIRATION_SKEW).isBefore(now)) {
				repository.delete(key.getKeyID());
			} else {
				keys.add(key);
			}
		}

		if (CollectionUtils.isEmpty(keys)) {
			keys.add(repository.create(KeyAlgorithm.RS256, CRYPTO_PERIOD));
		}

		log.info("Successfully loaded {} JWK(s)", keys.size());

		return new JWKSet(keys);
	}

	private static @Nullable OffsetDateTime toExpirationTime(@Nullable Date date) {
		return date == null ? null : OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
	}

}
