package com.konfigyr.thymeleaf;

import org.springframework.validation.BindingResult;
import org.thymeleaf.testing.templateengine.exception.TestEngineExecutionException;
import org.thymeleaf.testing.templateengine.spring6.context.web.SpringMVCWebProcessingContextBuilder;
import org.thymeleaf.testing.templateengine.testable.ITest;
import org.thymeleaf.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Custom context builder that would create and initialize the {@link BindingResult} using the
 * <code>bindingErrors</code> context attribute specified in the test templates.
 * <p>
 * This implementation is borrowed from <a href="https://github.com/thymeleaf/thymeleaf-tests/">thymeleaf-tests</a>
 * repository. Checkout the <code>ErrorsSpringIntegrationWebProcessingContextBuilder</code> type for reference.
 *
 * @author Vladimir Spasic
 **/
class KonfigyrProcessingContextBuilder extends SpringMVCWebProcessingContextBuilder {

	@Override
	@SuppressWarnings("unchecked")
	protected void initBindingResult(
			String bindingVariableName, Object target, ITest test, BindingResult bindingResult,
			Locale locale, Map<String, Object> variables
	) {
		super.initBindingResult(bindingVariableName, target, test,
				bindingResult, locale, variables);

		final var bindingErrors = (List<Map<String, Object>>) variables.get("bindingErrors");

		if (bindingErrors != null) {
			for (final Map<String, Object> errors : bindingErrors) {
				final Object binding = errors.get("binding");
				if (binding != null) {
					if (StringUtils.equals(binding, bindingVariableName)) {
						// This error map applies to this binding variable
						final Object field = errors.get("field");
						final Object message = errors.get("message");

						if (message == null) {
							throw new TestEngineExecutionException(
									"Error specification does not include property 'message', which is mandatory");
						}

						if (field != null) {
							// Field error
							bindingResult.rejectValue(
									StringUtils.toString(field),
									"field-error-code",
									StringUtils.toString(message)
							);
						} else {
							// Global error
							bindingResult.reject("global-error-code", StringUtils.toString(message));
						}
					}
				}
			}
		}
	}
}
