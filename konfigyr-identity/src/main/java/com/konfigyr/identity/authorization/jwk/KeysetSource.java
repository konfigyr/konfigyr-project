package com.konfigyr.identity.authorization.jwk;

import com.konfigyr.crypto.*;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.Assert;

import java.util.List;

@Slf4j
@NullMarked
@RequiredArgsConstructor
public class KeysetSource implements JWKSource<SecurityContext>, InitializingBean {

	private final KeysetStore store;
	private final KeysetDefinition definition;

	@Nullable
	private JWKSource<@Nullable SecurityContext> delegate;

	@Override
	public void afterPropertiesSet() {
		this.delegate = load();
	}

	@Override
	public List<JWK> get(JWKSelector selector, @Nullable SecurityContext ctx) throws KeySourceException {
		Assert.state(delegate != null, "JWK source has not been initialized");
		return delegate.get(selector, ctx);
	}

	@Scheduled(cron = "0 0 * * * *")
	void reload() {
		this.delegate = load();
	}

	@SuppressWarnings("unchecked")
	private JWKSource<SecurityContext> load() {
		if (log.isDebugEnabled()) {
			log.debug("Loading JWKs from repository...");
		}

		Keyset keyset;

		try {
			keyset = store.read(definition.getName());
		} catch (CryptoException.KeysetNotFoundException ex) {
			keyset = store.create(CryptoProperties.PROVIDER_NAME, CryptoProperties.KEK_ID, definition);
		}

		if (keyset instanceof JWKSource<?> source) {
			log.info("Successfully loaded: {}", source);

			return (JWKSource<SecurityContext>) source;
		}

		throw new IllegalStateException("Keyset store loaded a keyset that is not a JWK source: " + keyset +
				". Please make sure that the keyset definition is configured correctly.");
	}

}
