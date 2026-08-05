package com.konfigyr.mcp.registry;

import com.konfigyr.mcp.annotation.McpComponent;
import com.konfigyr.mcp.annotation.McpResource;
import com.konfigyr.mcp.annotation.McpTool;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * {@link BeanPostProcessor} that scans every bean annotated with {@link McpComponent} for
 * methods annotated with {@link McpResource} or {@link McpTool}, and registers each match, along
 * with its owning bean instance, into the given {@link McpAnnotationRegistry} for later use when
 * building the MCP server's tool and resource specifications.
 * <p>
 * Beans not annotated with {@link McpComponent} are skipped before any method-level reflection
 * is done.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
@NullMarked
public final class McpAnnotationBeanPostProcessor implements BeanPostProcessor {

	/**
	 * Method-level annotation types this processor looks for on {@link McpComponent} beans.
	 */
	static final Set<Class<? extends Annotation>> targetAnnotationTypes = Set.of(McpResource.class, McpTool.class);

	private final McpAnnotationRegistry registry;

	/**
	 * @param registry registry that discovered {@link McpResource}/{@link McpTool} methods are
	 * registered into, can't be {@literal null}
	 */
	public McpAnnotationBeanPostProcessor(McpAnnotationRegistry registry) {
		this.registry = registry;
	}

	/**
	 * Reflects over every method of {@code bean} looking for {@link McpResource}/{@link McpTool}
	 * annotations, provided {@code bean}'s class is itself annotated with {@link McpComponent} -
	 * every other bean is returned unchanged without inspecting its methods.
	 * <p>
	 * Each match is registered into this processor's {@link McpAnnotationRegistry} together with
	 * {@code bean} and the matched {@link java.lang.reflect.Method}, so it can be invoked reflectively
	 * later when the MCP server handles a matching request.
	 *
	 * @param bean the bean instance being processed, can't be {@literal null}
	 * @param beanName the registered name of the bean, can't be {@literal null}
	 * @return {@code bean}, unmodified
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		final Class<?> beanClass = AopUtils.getTargetClass(bean);

		// Only scan beans that opted in via '@McpComponent'. This check skips the per-method
		// reflection below for every other bean in the application context.
		if (!MergedAnnotations.from(beanClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).isPresent(McpComponent.class)) {
			return bean;
		}

		// Scan all methods in the target bean class...
		ReflectionUtils.doWithMethods(beanClass, method -> {
			final MergedAnnotations annotations = MergedAnnotations.from(method,
					MergedAnnotations.SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none());

			// Find all the method annotations that may match our MCP-related annotation types...
			targetAnnotationTypes.forEach(annotationType -> {
				if (annotations.isPresent(annotationType)) {
					if (log.isDebugEnabled()) {
						log.debug("Found '@{}' MCP annotation on: {}", annotationType.getSimpleName(), method);
					}

					final MergedAnnotation<?> annotation = annotations.get(annotationType)
							.withNonMergedAttributes();

					registry.register(annotation, bean, method);
				}
			});
		});

		return bean;
	}

}
