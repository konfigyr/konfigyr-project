package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

import java.io.Serial;

/**
 * Exception thrown when a {@link Member} does not exist.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class MemberNotFoundException extends NamespaceException {

	@Serial
	private static final long serialVersionUID = 6228715535652450037L;

	/**
	 * Create new instance of the {@link MemberNotFoundException} when there are no
	 * {@link Member members} with the matching {@link EntityId entity identifier}.
	 *
	 * @param id member entity identifier, can't be {@code null}
	 */
	public MemberNotFoundException(@NonNull EntityId id) {
		super(HttpStatus.NOT_FOUND, "Could not find a member with the following identifier: " + id.serialize());
	}

}
