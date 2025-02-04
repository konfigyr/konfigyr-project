package com.konfigyr.security.access;

import org.springframework.lang.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

record KonfigyrAccessControl(
		@NonNull ObjectIdentity objectIdentity,
		@NonNull Collection<AccessGrant> grants
) implements MutableAccessControl {

	@Serial
	private static final long serialVersionUID = 3323743851560275339L;

	static AccessControl namespace(@NonNull Serializable id, @NonNull Collection<AccessGrant> grants) {
		return new KonfigyrAccessControl(id, ObjectIdentity.NAMESPACE_TYPE, grants);
	}

	KonfigyrAccessControl(@NonNull Serializable id, @NonNull String type, @NonNull Collection<AccessGrant> grants) {
		this(new ObjectIdentity(type, id), grants);
	}

	KonfigyrAccessControl(@NonNull ObjectIdentity objectIdentity, @NonNull Collection<AccessGrant> grants) {
		this.objectIdentity = objectIdentity;
		this.grants = new LinkedHashSet<>(grants);
	}

	@Override
	public void add(@NonNull AccessGrant grant) {
		this.grants.add(grant);
	}

	@Override
	public void remove(@NonNull AccessGrant grant) {
		this.grants.remove(grant);
	}

	@Override
	public boolean isGranted(@NonNull SecurityIdentity identity, @NonNull Collection<Serializable> permissions) {
		for (final AccessGrant grant : this) {
			if (identity.equals(grant.identity()) && permissions.contains(grant.permission())) {
				return true;
			}
		}

		return false;
	}

	@NonNull
	@Override
	public Iterator<AccessGrant> iterator() {
		return Set.copyOf(grants).iterator();
	}

	@Override
	public String toString() {
		return "AccessControl(" + objectIdentity + ", " + grants + ")";
	}
}
