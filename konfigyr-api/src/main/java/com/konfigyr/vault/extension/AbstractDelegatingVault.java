package com.konfigyr.vault.extension;

import com.konfigyr.namespace.Service;
import com.konfigyr.vault.*;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

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
	public Properties state() {
		return delegate.state();
	}

	@Override
	public Map<String, String> unseal() {
		return delegate.unseal();
	}

	@Override
	public PropertyValue seal(PropertyValue property) {
		return delegate.seal(property);
	}

	@Override
	public PropertyValue unseal(PropertyValue property) {
		return delegate.unseal(property);
	}

	@Override
	public ApplyResult apply(PropertyChanges changes) {
		return delegate.apply(changes);
	}

	@Override
	public Vault submit(PropertyChanges changes) {
		return delegate.submit(changes);
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
