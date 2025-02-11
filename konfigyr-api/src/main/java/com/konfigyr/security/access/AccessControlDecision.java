package com.konfigyr.security.access;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.authorization.AuthorizationDecision;

import java.io.Serializable;
import java.util.Set;

/**
 * Represents an {@link AuthorizationDecision} based on permissions that are granted to {@link ObjectIdentity}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class AccessControlDecision extends AuthorizationDecision {

	private final ObjectIdentity identity;
	private final Set<Serializable> permissions;

	public AccessControlDecision(boolean granted, ObjectIdentity identity, Serializable... permissions) {
		this(granted, identity, Set.of(permissions));
	}

	public AccessControlDecision(boolean granted, ObjectIdentity identity, Set<Serializable> permissions) {
		super(granted);
		this.identity = identity;
		this.permissions = permissions;
	}

}
