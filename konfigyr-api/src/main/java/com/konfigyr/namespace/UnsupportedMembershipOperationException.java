package com.konfigyr.namespace;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when an operation on a {@link Member} of a {@link Namespace} can not be performed. Usually
 * when removing the last member with administrator role.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class UnsupportedMembershipOperationException extends NamespaceException {

	@Serial
	private static final long serialVersionUID = 7993940604122656981L;

	public UnsupportedMembershipOperationException(String message) {
		super(HttpStatus.BAD_REQUEST, message);
	}
}
