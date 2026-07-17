package com.konfigyr.artifactory;

import com.konfigyr.entity.EntityId;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class Owners {

	private static final Owner JOHN_DOE = new Owner(EntityId.from(1), "john-doe");
	private static final Owner KONFIGYR = new Owner(EntityId.from(2), "konfigyr");
	private static final Owner EBF = new Owner(EntityId.from(3), "ebf");

	public static Owner konfigyr() {
		return KONFIGYR;
	}

	public static Owner johnDoe() {
		return JOHN_DOE;
	}

	public static Owner ebf() {
		return EBF;
	}

}
