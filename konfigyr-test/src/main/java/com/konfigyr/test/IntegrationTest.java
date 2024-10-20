package com.konfigyr.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.core.annotation.AliasFor;

/**
 * Used to annotate tests to apply a <code>test</code> Spring profile as the active one.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@TestProfile
@ImportTestcontainers(TestContainers.class)
public @interface IntegrationTest {

	/**
	 * Which component classes to use for loading the Spring {@link org.springframework.context.ApplicationContext}.
	 *
	 * @return the component classes used to load the application context
	 */
	@AliasFor(attribute = "classes", annotation = SpringBootTest.class)
	String[] value() default {};

}
