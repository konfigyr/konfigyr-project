package com.konfigyr.security.access;

import com.konfigyr.entity.EntityId;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import java.io.Serial;

@EqualsAndHashCode
@RequiredArgsConstructor
final class SecurityEntityIdentity implements SecurityIdentity {

	@Serial
	private static final long serialVersionUID = 8676029914780480258L;

	private final EntityId id;

	@NonNull
	@Override
	public String get() {
		return id.serialize();
	}

	@Override
	public String toString() {
		return "SecurityIdentity(account=" + id + ")";
	}
}
