package com.konfigyr.security.access;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.support.NoOpCache;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;

import java.io.Serializable;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
class KonfigyrAccessService implements AccessService {

	private final AccessControlRepository accessControlRepository;
	private final Cache cache;

	KonfigyrAccessService(AccessControlRepository accessControlRepository) {
		this(accessControlRepository, new NoOpCache("access-controls"));
	}

	@Override
	public boolean hasAccess(@NonNull Authentication authentication, @NonNull String namespace) {
		return check(authentication, namespace, NamespaceRole.USER, NamespaceRole.ADMIN);
	}

	@Override
	public boolean hasAccess(@NonNull Authentication authentication, @NonNull String namespace, @NonNull NamespaceRole role) {
		return check(authentication, namespace, role);
	}

	private boolean check(@NonNull Authentication authentication, @NonNull String namespace, @NonNull Serializable... permissions) {
		final SecurityIdentity securityIdentity = resolveSecurityIdentity(authentication);
		final ObjectIdentity objectIdentity = ObjectIdentity.namespace(namespace);

		if (securityIdentity == null) {
			return false;
		}

		final AccessControl accessControl = getAccessControl(objectIdentity);

		if (accessControl == null) {
			return true;
		}

		if (log.isDebugEnabled()) {
			log.debug("Checking if at least one permission out of {} is granted to {} using {}",
					permissions, securityIdentity, accessControl);
		}

		return accessControl.isGranted(securityIdentity, Set.of(permissions));
	}

	@Nullable
	private AccessControl getAccessControl(@NonNull ObjectIdentity object) {
		final Cache.ValueWrapper value = cache.get(object);

		if (value != null) {
			return (AccessControl) value.get();
		}

		AccessControl accessControl = null;

		synchronized (cache) {
			try {
				accessControl = accessControlRepository.get(object);
			} catch (ObjectIdentityNotFound ex) {
				log.debug("Object identity does not exist, can not find access control for {}", object);
			} catch (Exception ex) {
				throw new AccessControlException("Failed to retrieve access control for " + object, ex);
			}

			cache.put(object, accessControl);

			return accessControl;
		}
	}

	@Nullable
	private static SecurityIdentity resolveSecurityIdentity(@NonNull Authentication authentication) {
		final EntityId id;

		try {
			id = EntityId.from(authentication.getName());
		} catch (IllegalArgumentException ex) {
			return null;
		}

		return SecurityIdentity.of(id);
	}

}
