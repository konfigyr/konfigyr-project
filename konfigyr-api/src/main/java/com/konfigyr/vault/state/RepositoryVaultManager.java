package com.konfigyr.vault.state;

import com.konfigyr.crypto.KeysetDefinition;
import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.crypto.KeysetOperationsFactory;
import com.konfigyr.crypto.tink.TinkAlgorithm;
import com.konfigyr.namespace.Service;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.vault.Profile;
import com.konfigyr.vault.Vault;
import com.konfigyr.vault.VaultAccessor;
import com.konfigyr.vault.VaultExtension;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@Slf4j
@NullMarked
@RequiredArgsConstructor
public class RepositoryVaultManager implements VaultAccessor {

	private final VaultExtension extension;
	private final Path repositoryLocation;
	private final KeysetOperationsFactory keysetOperationsFactory;

	@Override
	public Vault open(AuthenticatedPrincipal principal, Service service, Profile profile) {
		final KeysetOperations keysetOperations = keysetOperationsFactory.create(
				KeysetDefinition.of("vault/" + service.id().serialize(), TinkAlgorithm.AES256_GCM)
		);

		final Vault vault = RepositoryVault.builder()
				.author(principal)
				.service(service)
				.profile(profile)
				.keysetOperations(keysetOperations)
				.repository(GitStateRepository.load(service, repositoryLocation))
				.build();

		return extension.extend(vault);
	}

}
