package com.konfigyr.account;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;

/**
 * Event that is published when the {@link Account} was successfully registered in our system.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class AccountRegisteredEvent extends EntityEvent {

	public AccountRegisteredEvent(EntityId id) {
		super(id);
	}

}
