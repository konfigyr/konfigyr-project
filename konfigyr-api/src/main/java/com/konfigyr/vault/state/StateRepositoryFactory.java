package com.konfigyr.vault.state;

import com.konfigyr.namespace.Service;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface StateRepositoryFactory {

	StateRepository get(Service service);

}
