package com.konfigyr.artifactory;

import com.konfigyr.entity.EntityId;

public final class Owners {

	private static final Owner JOHN_DOE = new Owner(EntityId.from(1), "john-doe");
	private static final Owner KONFIGYR = new Owner(EntityId.from(2), "konfigyr");

	public static Owner konfigyr() {
		return KONFIGYR;
	}

	public static Owner johnDoe() {
		return JOHN_DOE;
	}

}
