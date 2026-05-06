package com.konfigyr.vault.environment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "konfigyr.vault.cache")
class ConfigurationCacheProperties {

	/**
	 * The Caffeine specification to use to create caches. Defaults to {@code maximumWeight} of
	 * 128MB where cached entries expire after last access in 30 minutes.
	 */
	@NotBlank
	private String specification = "maximumWeight=128000000,expireAfterAccess=30m";

}
