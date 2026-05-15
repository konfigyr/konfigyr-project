package com.konfigyr.identity.authorization.client;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.function.Function;

/**
 * A {@link RegisteredClientRepository} that caches lookups by both registration ID and client ID
 * in a single Spring {@link Cache}, using prefixed string keys to avoid collisions between the
 * two lookup strategies.
 * <p>
 * On any cache miss, the result is stored under both the {@code "id:<registrationId>"} and
 * {@code "client-id:<clientId>"} keys so that later lookups by either key are served from
 * cache without an additional delegate call.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@RequiredArgsConstructor
public class CachingRegisteredClientRepository implements RegisteredClientRepository {

	private static final String ID_PREFIX = "id:";
	private static final String CLIENT_ID_PREFIX = "client-id:";

	private final Cache cache;
	private final RegisteredClientRepository delegate;

	@Override
	public void save(RegisteredClient registeredClient) {
		delegate.save(registeredClient);
	}

	@Override
	@Nullable
	public RegisteredClient findById(String id) {
		return lookup(ID_PREFIX, id, delegate::findById);
	}

	@Override
	@Nullable
	public RegisteredClient findByClientId(String clientId) {
		return lookup(CLIENT_ID_PREFIX, clientId, delegate::findByClientId);
	}

	@Nullable
	private RegisteredClient lookup(String prefix, String id, Function<String, @Nullable RegisteredClient> supplier) {
		final String cacheKey = prefix + id;
		final Cache.ValueWrapper cached = cache.get(cacheKey);

		if (cached != null) {
			return (RegisteredClient) cached.get();
		}

		final RegisteredClient client = supplier.apply(id);

		if (client != null) {
			cache.put(ID_PREFIX + client.getId(), client);
			cache.put(CLIENT_ID_PREFIX + client.getClientId(), client);
		} else {
			cache.put(cacheKey, null);
		}

		return client;
	}

}
