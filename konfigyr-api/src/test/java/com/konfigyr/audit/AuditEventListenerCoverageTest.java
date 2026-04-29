package com.konfigyr.audit;

import com.konfigyr.vault.ChangeRequestEvent;
import org.jmolecules.event.annotation.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architecture test that verifies every {@link DomainEvent}-annotated class in the
 * {@code com.konfigyr} package hierarchy has a corresponding audit listener method
 * in {@link AuditEventListener}.
 * <p>
 * This test acts as a safety net: when a developer introduces a new domain event subclass,
 * this test will fail until the corresponding audit mapping is added to the listener.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
class AuditEventListenerCoverageTest {

	@Test
	@DisplayName("every @DomainEvent class should have a corresponding audit listener method")
	void shouldHaveAuditMappingForAllDomainEvents() {
		final Set<Class<?>> domainEventClasses = findDomainEventClasses();
		final Set<Class<?>> auditedEventClasses = findAuditedEventClasses();

		assertThat(domainEventClasses)
				.as("Expected to find @DomainEvent-annotated classes in com.konfigyr")
				.isNotEmpty();

		final Set<String> unaudited = domainEventClasses.stream()
				.filter(it -> !auditedEventClasses.contains(it))
				.map(Class::getName)
				.collect(Collectors.toSet());

		assertThat(unaudited)
				.as("The following @DomainEvent classes have no audit listener in AuditEventListener. " +
						"Add a @TransactionalEventListener method for each.")
				.isEmpty();
	}

	/**
	 * Scans the classpath for all concrete classes annotated with {@link DomainEvent}
	 * under the {@code com.konfigyr} base package.
	 */
	private static Set<Class<?>> findDomainEventClasses() {
		final var scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(DomainEvent.class));
		scanner.addExcludeFilter(new AssignableTypeFilter(ChangeRequestEvent.class));

		return scanner.findCandidateComponents("com.konfigyr").stream()
				.map(bd -> {
					try {
						return Class.forName(bd.getBeanClassName());
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				})
				.collect(Collectors.toSet());
	}

	/**
	 * Collects all event classes referenced in {@link EventListener#classes()}
	 * annotations on methods of {@link AuditEventListener}.
	 */
	private static Set<Class<?>> findAuditedEventClasses() {
		return Arrays.stream(AuditEventListener.class.getDeclaredMethods())
				.map(MergedAnnotations::from)
				.filter(annotations -> annotations.isPresent(EventListener.class))
				.map(annotations -> annotations.get(EventListener.class))
				.flatMap(annotation -> Arrays.stream(annotation.getClassArray("classes")))
				.collect(Collectors.toSet());
	}

}
