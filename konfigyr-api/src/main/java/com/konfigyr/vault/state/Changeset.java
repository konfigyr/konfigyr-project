package com.konfigyr.vault.state;

import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.vault.Properties;
import com.konfigyr.vault.PropertyChange;
import com.konfigyr.vault.PropertyChanges;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.io.InputStreamSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;

@NullMarked
@ValueObject
public record Changeset(
		AuthenticatedPrincipal author,
		Properties properties,
		PropertyChanges changes
) implements InputStreamSource, Iterable<PropertyChange>, Serializable {

	@Serial
	private static final long serialVersionUID = 2754166536195908346L;

	@Override
	public Iterator<PropertyChange> iterator() {
		return changes.iterator();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return properties.getInputStream();
	}
}
