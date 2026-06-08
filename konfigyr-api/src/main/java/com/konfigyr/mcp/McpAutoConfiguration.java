package com.konfigyr.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.konfigyr.artifactory.Artifactory;
import com.konfigyr.artifactory.ArtifactoryAutoConfiguration;
import com.konfigyr.namespace.NamespaceManagementAutoConfiguration;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.Services;
import org.jspecify.annotations.NullMarked;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerJsonMapperAutoConfiguration;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Autoconfiguration that registers all Konfigyr MCP beans explicitly.
 * <p>
 * Rather than relying on classpath scanning to discover tool classes, every MCP bean is
 * declared here as a {@link Bean @Bean} method. This keeps the MCP wiring visible and
 * auditable in a single place: the namespace context, each tool group, and the corresponding
 * {@link ToolCallbackProvider} that surfaces them to the Spring AI MCP server.
 * <p>
 * <b>Adding a new tool group</b> follows a two-step pattern:
 * <ol>
 *   <li>Create a package-private class with {@link org.springframework.ai.tool.annotation.Tool @Tool}-annotated methods
 *       and declare a {@link Bean @Bean} method for it here, injecting its dependencies explicitly.</li>
 *   <li>Add the new bean as a parameter to {@link #konfigyrToolCallbacks} and chain a
 *       {@code .register(...)} call in the {@link ToolCallbackProviderBuilder}.</li>
 * </ol>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see NamespaceContext
 * @see ArtifactoryTools
 */
@NullMarked
@AutoConfiguration
@AutoConfigureAfter({
		ArtifactoryAutoConfiguration.class,
		NamespaceManagementAutoConfiguration.class
})
@AutoConfigureBefore({
		McpServerJsonMapperAutoConfiguration.class
})
public class McpAutoConfiguration {

	/**
	 * Provides the MCP server's dedicated {@link JsonMapper} by extending the application's
	 * primary {@link JsonMapper} with the configuration required by the MCP protocol.
	 * <p>
	 * The MCP server autoconfiguration ({@code McpServerJsonMapperAutoConfiguration}) builds its
	 * own isolated mapper that discovers Jackson modules only via the service loader, not from
	 * Spring beans. Because the artifactory module and other Spring-registered Jackson modules
	 * are not registered under {@code META-INF/services}, they would be absent from the default
	 * MCP mapper, causing incorrect serialization of {@link com.konfigyr.artifactory.JsonSchema}
	 * subtypes returned by the {@code searchProperties} tool.
	 * <p>
	 * This bean clones the primary {@link JsonMapper} (which already carries all Spring-registered
	 * modules) via {@link JsonMapper#rebuild()} and layers the same MCP-specific configuration
	 * that {@code McpServerJsonMapperAutoConfiguration} would apply:
	 * <ul>
	 *   <li>{@link DeserializationFeature#ACCEPT_EMPTY_STRING_AS_NULL_OBJECT}</li>
	 *   <li>{@link SerializationFeature#FAIL_ON_EMPTY_BEANS} disabled</li>
	 *   <li>{@link JsonInclude.Include#NON_NULL} for both value and content inclusion</li>
	 * </ul>
	 * <p>
	 * The {@code defaultCandidate = false} attribute mirrors the upstream convention so that this
	 * mapper is never injected at unqualified injection points.
	 *
	 * @param jsonMapper the primary application mapper, never {@literal null}
	 * @return the MCP-specific {@link JsonMapper}, never {@literal null}
	 */
	@Bean(name = "mcpServerJsonMapper", defaultCandidate = false)
	JsonMapper mcpServerJsonMapper(JsonMapper jsonMapper) {
		return jsonMapper.rebuild()
				.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
				.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
				.changeDefaultPropertyInclusion(inclusion -> inclusion
						.withValueInclusion(JsonInclude.Include.NON_NULL)
						.withContentInclusion(JsonInclude.Include.NON_NULL)
				).build();
	}

	/**
	 * Registers the request-scoped {@link NamespaceContext} that resolves the target
	 * {@link com.konfigyr.namespace.Namespace} for each MCP tool invocation from the
	 * authenticated OAuth2 client principal.
	 * <p>
	 * The {@link ScopedProxyMode#INTERFACES} proxy ensures that singleton-scoped tool beans
	 * receive a proxy that delegates to the correct request-bound instance at call time.
	 *
	 * @param namespaces the namespace manager used for client ID resolution, never {@literal null}
	 * @return request-scoped namespace context, never {@literal null}
	 */
	@Bean
	@RequestScope(proxyMode = ScopedProxyMode.INTERFACES)
	NamespaceContext namespaceContext(NamespaceManager namespaces) {
		return new McpNamespaceContext(namespaces);
	}

	/**
	 * Declares the {@link ArtifactoryTools} bean that backs the {@code searchProperties}
	 * MCP tool.
	 *
	 * @param namespaceContext the request-scoped namespace context, never {@literal null}
	 * @param services         the namespace service registry, never {@literal null}
	 * @param artifactory      the artifactory domain service, never {@literal null}
	 * @return the artifactory tools bean, never {@literal null}
	 */
	@Bean
	ArtifactoryTools artifactoryTools(NamespaceContext namespaceContext, Services services, Artifactory artifactory) {
		return new ArtifactoryTools(namespaceContext, services, artifactory);
	}

	/**
	 * Registers all Konfigyr MCP tool groups with the Spring AI MCP server as a single
	 * {@link ToolCallbackProvider}.
	 * <p>
	 * Spring AI's {@code DefaultToolCallResultConverter} hardcodes its own Jackson mapper with
	 * default settings, so {@code spring.jackson.*} properties — such as
	 * {@code spring.jackson.default-property-inclusion=non_null} — are silently ignored and
	 * null fields appear in tool responses (see
	 * <a href="https://github.com/spring-projects/spring-ai/issues/6097">spring-ai#6097</a>).
	 * {@link ToolCallbackProviderBuilder} sidesteps this by wiring the {@code mcpServerJsonMapper}
	 * — derived from the application's primary mapper via {@link tools.jackson.databind.json.JsonMapper#rebuild()}
	 * — as the shared {@link org.springframework.ai.tool.execution.ToolCallResultConverter} for
	 * every registered tool method. This ensures that all {@code spring.jackson.*} settings and
	 * Spring-registered Jackson modules (including the Artifactory module) are applied when tool
	 * results are serialized.
	 * <p>
	 * <b>Adding a new tool group:</b> declare its {@link Bean @Bean} method in this class,
	 * add it as a parameter here, and chain an additional {@code .register(...)} call.
	 *
	 * @param mcpServerJsonMapper the MCP-specific mapper derived from the application mapper,
	 *                            resolved by name, never {@literal null}
	 * @param artifactoryTools    the Artifactory tool group, never {@literal null}
	 * @return the single {@link ToolCallbackProvider} exposing all Konfigyr MCP tools,
	 *         never {@literal null}
	 */
	@Bean
	ToolCallbackProvider konfigyrToolCallbacks(
			@Qualifier("mcpServerJsonMapper") JsonMapper mcpServerJsonMapper,
			ArtifactoryTools artifactoryTools
	) {
		return new ToolCallbackProviderBuilder(mcpServerJsonMapper)
				.register(artifactoryTools)
				.build();
	}

}
