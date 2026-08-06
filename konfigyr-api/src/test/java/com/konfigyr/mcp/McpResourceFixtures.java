package com.konfigyr.mcp;

import com.konfigyr.mcp.annotation.McpResource;
import com.konfigyr.mcp.annotation.McpTemplateVariable;
import com.konfigyr.mcp.registry.McpAnnotationRegistration;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Reusable {@code @McpResource}-annotated fixture methods, shared across tests that need a real
 * annotated resource method to reflect over - building a {@code McpAnnotationRegistration}, a
 * {@code McpHandlerMethod}, or a full resource specification - rather than each test declaring
 * its own throwaway fixture bean.
 */
public final class McpResourceFixtures {

	/**
	 * The shared bean instance every fixture method below is declared on.
	 */
	public static final ResourceBean BEAN = new ResourceBean();

	private McpResourceFixtures() {
	}

	/**
	 * Looks up one of {@link ResourceBean}'s declared methods by name.
	 *
	 * @param name the method name, can't be {@literal null}
	 * @return the method, never {@literal null}
	 */
	public static Method method(String name) {
		return Arrays.stream(ResourceBean.class.getDeclaredMethods())
				.filter(candidate -> candidate.getName().equals(name))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No fixture resource method named '" + name + "'"));
	}

	/**
	 * Builds a {@link McpAnnotationRegistration} for one of {@link ResourceBean}'s declared
	 * methods, as if it had been discovered by {@code McpAnnotationBeanPostProcessor}.
	 *
	 * @param name the method name, can't be {@literal null}
	 * @return the registration, never {@literal null}
	 */
	public static McpAnnotationRegistration<McpResource> registration(String name) {
		final Method method = method(name);
		return new McpAnnotationRegistration<>(BEAN, method, method.getAnnotation(McpResource.class));
	}

	public record Manifest(String service, List<String> artifacts) {

	}

	public static class ResourceBean {

		@McpResource(
				uri = "konfigyr://services/{service}/manifest",
				name = "service_manifest",
				title = "Service Manifest",
				description = "Current manifest of a service"
		)
		public Manifest manifest(@McpTemplateVariable("service") String service) {
			return new Manifest(service, List.of("artifact-a", "artifact-b"));
		}

		@McpResource(
				uri = "konfigyr://artifacts/{groupId}/{artifactId}",
				name = "artifact_metadata",
				title = "Artifact metadata",
				description = "Metadata for one artifact",
				mimeType = "application/json"
		)
		public Manifest artifact(
				@McpTemplateVariable("groupId") String groupId,
				@McpTemplateVariable("artifactId") String artifactId
		) {
			return new Manifest(groupId + ":" + artifactId, List.of());
		}

	}

}
