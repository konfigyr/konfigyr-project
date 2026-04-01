package com.konfigyr.data.web;

import com.konfigyr.data.CursorPageable;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Extracts the cursor based paging information from web requests and thus allows injecting the
 * {@link CursorPageable} instances into controller methods. Request properties to be parsed can
 * be configured defaulting to {@code token} for the continuation token and {@code size} for the
 * page size.
 * <p>
 * Parameters can be {@link #setPrefix(String) prefixed} to disambiguate from other parameters in
 * the request if necessary.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Getter
@Setter
@NullMarked
public class CursorPageableHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private static final String DEFAULT_TOKEN_PARAMETER = "token";
	private static final String DEFAULT_SIZE_PARAMETER = "size";
	private static final String DEFAULT_PREFIX = "";
	private static final String DEFAULT_QUALIFIER_DELIMITER = "_";
	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int DEFAULT_MAX_PAGE_SIZE = 2000;

	private String tokenParameterName = DEFAULT_TOKEN_PARAMETER;
	private String sizeParameterName = DEFAULT_SIZE_PARAMETER;
	private String prefix = DEFAULT_PREFIX;
	private String qualifierDelimiter = DEFAULT_QUALIFIER_DELIMITER;
	private int defaultPageSize = DEFAULT_PAGE_SIZE;
	private int maxPageSize = DEFAULT_MAX_PAGE_SIZE;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return ClassUtils.isAssignable(CursorPageable.class, parameter.getParameterType());
	}

	@Override
	public @Nullable CursorPageable resolveArgument(
			MethodParameter parameter,
			@Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest,
			@Nullable WebDataBinderFactory binderFactory
	) {
		final String tokenParameter = webRequest.getParameter(getParameterNameToUse(getTokenParameterName(), parameter));
		final String sizeParameter = webRequest.getParameter(getParameterNameToUse(getSizeParameterName(), parameter));
		return CursorPageable.of(tokenParameter, resolvePageSize(sizeParameter));
	}

	/**
	 * Returns the name of the request parameter to find the {@link CursorPageable} information in.
	 * Inspects the given {@link MethodParameter} for {@link Qualifier} present and prefixes the given
	 * source parameter name with it.
	 *
	 * @param source the basic parameter name.
	 * @param parameter the {@link MethodParameter} potentially qualified.
	 * @return the name of the request parameter.
	 */
	private String getParameterNameToUse(String source, @Nullable MethodParameter parameter) {
		final StringBuilder builder = new StringBuilder(getPrefix());

		if (parameter != null) {
			final MergedAnnotations annotations = MergedAnnotations.from(parameter.getParameter());
			final MergedAnnotation<Qualifier> qualifier = annotations.get(Qualifier.class);

			if (qualifier.isPresent()) {
				builder.append(qualifier.getString("value"))
						.append(getQualifierDelimiter());
			}
		}

		return builder.append(source).toString();
	}

	private int resolvePageSize(@Nullable String sizeParameter) {
		int size = getDefaultPageSize();

		if (StringUtils.hasText(sizeParameter)) {
			try {
				size = Integer.parseInt(sizeParameter);
			} catch (NumberFormatException e) {
				// ignore and use default...
			}
		}

		return Math.min(size, getMaxPageSize());
	}
}
