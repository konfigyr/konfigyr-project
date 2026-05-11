package com.konfigyr.test.observation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Annotation that enables comprehensive testing support for Spring Observations.
 * <p>
 * This annotation performs two primary functions:
 * <ul>
 *     <li>
 *         Imports the {@link TestObservationConfiguration} to provide a
 *         {@link io.micrometer.observation.tck.TestObservationRegistry} Spring Bean.
 *     </li>
 *     <li>
 *         Registers the {@link org.springframework.context.ApplicationListener} for
 *         {@link org.springframework.test.context.event.BeforeTestExecutionEvent} to ensure
 *         the registry is automatically cleared between test methods, preventing state leakage.
 *    </li>
 * </ul>
 *
 * Use this on any {@code @SpringBootTest} or {@code @ContextConfiguration} test class
 * where you need to assert metrics, traces, or metadata via the Observation API like so:
 * <pre>
 * &#64;SpringBootTest
 * &#64;TestObservations
 * class MyObservationTest { ... }
 * </pre>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(TestObservationConfiguration.class)
public @interface TestObservations {

}
