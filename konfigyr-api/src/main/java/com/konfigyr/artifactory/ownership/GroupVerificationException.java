package com.konfigyr.artifactory.ownership;

import java.io.Serial;

public class GroupVerificationException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = -7890505623131658890L;

	public GroupVerificationException(String message) {
		super(message);
	}

	public GroupVerificationException(String message, Throwable cause) {
		super(message, cause);
	}

}
