package com.konfigyr.test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Used to annotate tests to apply a <code>test</code> Spring profile as the active one.
 *
 * @author : Vladimir Spasic
 * @since 1.0.0
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ActiveProfiles
public @interface TestProfile {

	/**
	 * The Spring Boot profiles to activate. Defaults to <code>test</code>.
	 *
	 * @return profile names
	 */
	@AliasFor(attribute = "profiles", annotation = ActiveProfiles.class)
	String[] value() default "test";

}
