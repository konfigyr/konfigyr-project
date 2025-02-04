package com.konfigyr.security.access;

import com.konfigyr.entity.EntityId;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;

@EqualsAndHashCode
@RequiredArgsConstructor
final class SecurityEntityIdentity implements SecurityIdentity {

	private final EntityId id;

	@NonNull
	@Override
	public String get() {
		return id.serialize();
	}

	@Override
	public String toString() {
		return "SecurityIdentity(" + id + ")";
	}
}
