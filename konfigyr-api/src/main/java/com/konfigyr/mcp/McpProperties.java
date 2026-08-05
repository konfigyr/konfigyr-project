package com.konfigyr.mcp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties type used to configure the Konfigyr MCP server.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties("konfigyr.mcp")
class McpProperties {

	/**
	 * Enable/disable the Konfigyr MCP server.
	 * <p>
	 * When set to {@code false}, the {@code /mcp} endpoint and all of its components are not registered.
	 */
	private boolean enabled = true;

	/**
	 * Name of this MCP server, reported to clients during the {@code initialize} handshake.
	 */
	@NotBlank
	private String name = "konfigyr-mcp";

	/**
	 * Version of this MCP server, reported to clients during the {@code initialize} handshake.
	 */
	@NotBlank
	private String version;

	/**
	 * Human-readable description of this MCP server, reported to clients during the {@code initialize}
	 * handshake.
	 */
	private String description;

	/**
	 * URL of a website with more information about this MCP server, reported to clients during the
	 * {@code initialize} handshake.
	 */
	private String websiteUrl;

	/**
	 * Instructions shared with MCP clients during the {@code initialize} handshake, describing
	 * the server's tools and resources and how they are meant to be used together.
	 */
	@NotBlank
	private String instructions;

	/**
	 * Which capabilities this server declares to clients during the {@code initialize} handshake.
	 */
	@NestedConfigurationProperty
	private Capabilities capabilities = new Capabilities();

	/**
	 * Toggles for the individual MCP capabilities this server may declare. A capability is only
	 * declared to clients when its flag is {@code true} <em>and</em> this server actually registers
	 * something backing it. Flipping a flag here does not, by itself, create tools/resources/prompts.
	 */
	@Data
	public static class Capabilities {

		/**
		 * Declare the MCP tools capability.
		 */
		private boolean tool = true;

		/**
		 * Declare the MCP resources capability.
		 */
		private boolean resource = true;

		/**
		 * Declare the MCP prompts capability.
		 */
		private boolean prompt = false;

		/**
		 * Declare the MCP completions capability.
		 */
		private boolean completion = false;

	}

}
