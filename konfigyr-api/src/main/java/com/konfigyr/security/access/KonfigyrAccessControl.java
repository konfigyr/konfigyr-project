package com.konfigyr.security.access;

import org.jspecify.annotations.NullMarked;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

@NullMarked
record KonfigyrAccessControl(
		ObjectIdentity objectIdentity,
		Collection<AccessGrant> grants
) implements MutableAccessControl {

	@Serial
	private static final long serialVersionUID = 3323743851560275339L;

	static AccessControl namespace(Serializable id, Collection<AccessGrant> grants) {
		return new KonfigyrAccessControl(id, ObjectIdentity.NAMESPACE_TYPE, grants);
	}

	KonfigyrAccessControl(Serializable id, String type, Collection<AccessGrant> grants) {
		this(new ObjectIdentity(type, id), grants);
	}

	KonfigyrAccessControl(ObjectIdentity objectIdentity, Collection<AccessGrant> grants) {
		this.objectIdentity = objectIdentity;
		this.grants = new LinkedHashSet<>(grants);
	}

	@Override
	public void add(AccessGrant grant) {
		this.grants.add(grant);
	}

	@Override
	public void remove(AccessGrant grant) {
		this.grants.remove(grant);
	}

	@Override
	public boolean isGranted(SecurityIdentity identity, Collection<Serializable> permissions) {
		for (final AccessGrant grant : this) {
			if (identity.equals(grant.identity()) && permissions.contains(grant.permission())) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Iterator<AccessGrant> iterator() {
		return Set.copyOf(grants).iterator();
	}

	@Override
	public String toString() {
		return "AccessControl(" + objectIdentity + ", " + grants + ")";
	}
}
