package com.konfigyr.web.error;

import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.lang.NonNull;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

/**
 * Extend the default behaviour of {@link DefaultErrorAttributes} to include authentication errors
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
