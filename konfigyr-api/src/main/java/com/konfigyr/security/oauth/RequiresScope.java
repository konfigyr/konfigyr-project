package com.konfigyr.security.oauth;

import com.konfigyr.security.OAuthScope;

import java.lang.annotation.*;

/**
 * Annotation for specifying a method access-control expression which will check if the current
 * OAuth Client was granted required {@link OAuthScope OAuth scopes}.
 *
 * @author Vladimit Spasic
 * @since 1.0
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresScope {

	/**
	 * Specify which {@link OAuthScope OAuth scopes or scope} is required to invoke this method,
	 * or when this annotation is specified on a type, all methods of the given type.
	 *
	 * @return required OAuth scopes.
	 */
	OAuthScope[] value();

}
