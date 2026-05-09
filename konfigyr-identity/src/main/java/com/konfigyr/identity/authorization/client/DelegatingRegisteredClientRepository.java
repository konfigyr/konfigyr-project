package com.konfigyr.identity.authorization.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;

/**
 * Implementation of a {@link RegisteredClientRepository} that delegates the lookup of {@link RegisteredClient}(s)
 * to a collection of {@link RegisteredClientRepository} implementations.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
public final class DelegatingRegisteredClientRepository implements RegisteredClientRepository {

	private final Collection<RegisteredClientRepository> delegates;

	public DelegatingRegisteredClientRepository(Collection<RegisteredClientRepository> delegates) {
		Assert.notEmpty(delegates, "Delegating client repositories cannot be empty");
		this.delegates = Collections.unmodifiableCollection(delegates);
	}

	@Override
	public void save(RegisteredClient registeredClient) {
		// this is a noop, since we don't support registering OAuth clients
	}

	@Override
	public RegisteredClient findById(String id) {
		return lookup(RegisteredClientRepository::findById, id);
	}

	@Override
	public RegisteredClient findByClientId(String clientId) {
		return lookup(RegisteredClientRepository::findByClientId, clientId);
	}

	private RegisteredClient lookup(BiFunction<RegisteredClientRepository, String, RegisteredClient> lookup, String id) {
		if (StringUtils.isBlank(id)) {
			return null;
		}

		Exception lastException = null;

		for (RegisteredClientRepository delegate : this.delegates) {
			final RegisteredClient client;

			try {
				client = lookup.apply(delegate, id);
			} catch (Exception ex) {
				log.debug("Failed to lookup registered OAuth client using repository: {}",
						ClassUtils.getUserClass(delegate), ex);

				lastException = ex;
				continue;
			}

			if (client != null) {
				return client;
			}
		}

		if (lastException != null) {
			if (lastException instanceof AuthenticationException ex) {
				throw ex;
			}

			throw new InternalAuthenticationServiceException("Failed to lookup registered OAuth client", lastException);
		}

		return null;
	}
}
