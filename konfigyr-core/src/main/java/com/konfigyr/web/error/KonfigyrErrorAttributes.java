package com.konfigyr.web.error;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

/**
 * Extend the default behavior of {@link DefaultErrorAttributes} to include authentication errors
 * when generating the error model.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class KonfigyrErrorAttributes extends DefaultErrorAttributes {

	@Override
	public Throwable getError(@NonNull WebRequest request) {
		Throwable throwable = super.getError(request);

		if (throwable == null) {
			throwable = (Throwable) request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, RequestAttributes.SCOPE_REQUEST);
		}

		return throwable;
	}
}
