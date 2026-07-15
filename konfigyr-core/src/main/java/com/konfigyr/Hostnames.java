package com.konfigyr;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;

/**
 * Container holding the base hostnames of the Konfigyr platform services.
 * <p>
 * Used to construct absolute URIs pointing at these services, for example, when linking back to the
 * Frontend application from an email or when retrieving resources from the REST API.
 *
 * @param api Absolute base URI of the Konfigyr REST API, defaults to the production Konfigyr API
 *            of https://api.konfigyr.com
 * @param identity Absolute base URI of the Konfigyr Identity Provider, defaults to the production
 *                 Konfigyr Identity Provider of https://id.konfigyr.com
 * @param web Absolute base URI of the Konfigyr Frontend application, defaults to the production
 *            Konfigyr Frontend application of https://konfigyr.com
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Validated
@NullMarked
@ConfigurationProperties("konfigyr.hostnames")
public record Hostnames(
		@NotNull @DefaultValue("https://api.konfigyr.com") URI api,
		@NotNull @DefaultValue("https://id.konfigyr.com") URI identity,
		@NotNull @DefaultValue("https://konfigyr.com") URI web
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 3988463525054447352L;

}
