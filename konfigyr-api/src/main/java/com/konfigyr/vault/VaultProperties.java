package com.konfigyr.vault;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Data
@Validated
@ConfigurationProperties(prefix = "konfigyr.vault")
public class VaultProperties {

	/**
	 * Directory where Git-based state repositories would be created and managed.
	 */
	@NotNull
	private Path repositoryDirectory;

}
