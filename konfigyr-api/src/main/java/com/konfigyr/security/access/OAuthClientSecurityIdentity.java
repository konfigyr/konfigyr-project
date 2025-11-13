package com.konfigyr.security.access;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;

import java.io.Serial;

@EqualsAndHashCode
@RequiredArgsConstructor
final class OAuthClientSecurityIdentity implements SecurityIdentity {

	@Serial
	private static final long serialVersionUID = 8025684936297221441L;

	private final String clientId;

	@NonNull
	@Override
	public String get() {
		return clientId;
	}

	@Override
	public String toString() {
		return "SecurityIdentity(client_id=" + clientId + ")";
	}
}
