package com.konfigyr.vault.extension;

import com.konfigyr.namespace.Service;
import com.konfigyr.vault.Profile;
import com.konfigyr.vault.Vault;
import org.jspecify.annotations.NullMarked;

@NullMarked
abstract class AbstractDelegatingVault implements Vault {

	protected final Vault delegate;

	protected AbstractDelegatingVault(Vault delegate) {
		this.delegate = delegate;
	}

	@Override
	public Service service() {
		return delegate.service();
	}

	@Override
	public Profile profile() {
		return delegate.profile();
	}

	@Override
	public void close() throws Exception {
		delegate.close();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public String toString() {
		return delegate.toString();
	}
}
