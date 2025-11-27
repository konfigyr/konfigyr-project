package com.konfigyr.web.filter;

import com.konfigyr.crypto.TokenGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Filter that would be extracting and storing relevant information about the incoming {@link HttpServletRequest}
 * in the {@link MDC logging context}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class ContextFilter extends OncePerRequestFilter {

	/**
	 * The default order for this filter in the filter chain.
	 */
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 90;

	static final String X_REQUEST_ID = "X-Request-ID";
	static final String X_FORWARDED_FOR = "X-Forwarded-For";

	private final RequestMatcher matcher;
	private final StringKeyGenerator generator;
	private final LocaleResolver localeResolver;

	public ContextFilter() {
		this(AnyRequestMatcher.INSTANCE, new AcceptHeaderLocaleResolver());
	}

	public ContextFilter(RequestMatcher matcher) {
		this(matcher, new AcceptHeaderLocaleResolver());
	}

	public ContextFilter(LocaleResolver localeResolver) {
		this(AnyRequestMatcher.INSTANCE, localeResolver);
	}

	public ContextFilter(RequestMatcher matcher, LocaleResolver localeResolver) {
		this(matcher, localeResolver, TokenGenerator.getInstance());
	}

	public ContextFilter(RequestMatcher matcher, LocaleResolver localeResolver, StringKeyGenerator generator) {
		this.matcher = matcher;
		this.generator = generator;
		this.localeResolver = localeResolver;
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request,
									@NonNull HttpServletResponse response,
									@NonNull FilterChain chain) throws ServletException, IOException {
		final String requestId = extractRequestId(generator, request);
		final LocaleContext localeContext = extractLocaleContext(localeResolver, request);

		response.addHeader(X_REQUEST_ID, requestId);
		LocaleContextHolder.setLocaleContext(localeContext);

		try (ClosableContext ignore = ClosableContext.create(requestId, localeContext, request)) {
			chain.doFilter(request, response);
		} finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		return !matcher.matches(request);
	}

	static void add(String key, Supplier<String> supplier, Consumer<Closeable> consumer) {
		final String value;

		try {
			value = supplier.get();
		} catch (Exception e) {
			return;
		}

		if (StringUtils.hasText(value)) {
			consumer.accept(MDC.putCloseable(key, value));
		}
	}

	@NonNull
	static LocaleContext extractLocaleContext(LocaleResolver resolver, HttpServletRequest request) {
		if (resolver instanceof LocaleContextResolver localeContextResolver) {
			return localeContextResolver.resolveLocaleContext(request);
		}
		return () -> resolver.resolveLocale(request);
	}

	@NonNull
	static String extractLocale(LocaleContext context) {
		Locale locale = context.getLocale();

		if (locale == null) {
			locale = Locale.US;
		}

		return locale.toString();
	}

	@NonNull
	static String extractRequestId(StringKeyGenerator generator, HttpServletRequest request) {
		String rid = request.getHeader(X_REQUEST_ID);

		if (!StringUtils.hasText(rid)) {
			try {
				rid = generator.generateKey();
			} catch (Exception e) {
				rid = UUID.randomUUID().toString();
			}
		}

		return rid;
	}

	@Nullable
	static String extractRequestMethod(HttpServletRequest request) {
		return request.getMethod();
	}

	@Nullable
	static String extractRequestUri(HttpServletRequest request) {
		return request.getRequestURI();
	}

	@NonNull
	static String extractHost(HttpServletRequest request) {
		final StringBuilder builder = new StringBuilder(request.getScheme())
				.append("://")
				.append(request.getServerName());

		final int port = request.getServerPort();

		if ((request.getScheme().equals("http") && port != 80) || (request.getScheme().equals("https") && port != 443)) {
			builder.append(':').append(port);
		}

		return builder.toString();
	}

	@Nullable
	static String extractRemoteAddress(HttpServletRequest request) {
		String address = request.getHeader(X_FORWARDED_FOR);

		if (!StringUtils.hasText(address)) {
			address = request.getRemoteAddr();
		}

		return address;
	}

	private record ClosableContext(Iterable<Closeable> closeables) implements Closeable {

		static ClosableContext create(@NonNull String rid, @NonNull LocaleContext localeContext, @NonNull HttpServletRequest request) {
			final List<Closeable> closeables = new ArrayList<>();
			add("rid", () -> rid, closeables::add);
			add("locale", () -> extractLocale(localeContext), closeables::add);
			add("ip", () -> extractRemoteAddress(request), closeables::add);
			add("host", () -> extractHost(request), closeables::add);
			add("method", () -> extractRequestMethod(request), closeables::add);
			add("uri", () -> extractRequestUri(request), closeables::add);
			return new ClosableContext(closeables);
		}

		@Override
		public void close() {
			for (Closeable closeable : closeables) {
				try {
					closeable.close();
				} catch (Exception e) {
					// ignore exceptions...
				}
			}
		}
	}

}
