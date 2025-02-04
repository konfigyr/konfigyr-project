package com.konfigyr.hateoas;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class UriComponentsLinkBuilder implements LinkBuilder {

	private UriComponentsBuilder builder;
	private HttpMethod method = HttpMethod.GET;
	private String title;
	private String type;
	private String name;
	private String deprecation;

	UriComponentsLinkBuilder() {
		this.builder = createUriComponentsBuilder();
	}

	UriComponentsLinkBuilder(UriComponents components) {
		this.builder = ServletUriComponentsBuilder.newInstance().uriComponents(components);
	}

	@NonNull
	@Override
	public LinkBuilder path(@Nullable Object object) {
		String path = extractObjectValue(object);

		if (path == null) {
			return this;
		}

		if (path.endsWith("#")) {
			path = path.substring(0, path.length() - 1);
		}

		if (StringUtils.isBlank(path)) {
			return this;
		}

		path = path.startsWith("/") ? path : "/".concat(path);
		builder = builder.path(path);

		return this;
	}

	@NonNull
	@Override
	public LinkBuilder query(@NonNull String name, @Nullable Object value) {
		String parameter = extractObjectValue(value);

		if (parameter != null) {
			builder = builder.queryParam(name, parameter);
		}

		return this;
	}

	@NonNull
	@Override
	public LinkBuilder query(@NonNull String name, @Nullable List<Object> values) {
		if (CollectionUtils.isEmpty(values)) {
			return this;
		}

		for (Object value : values) {
			query(name, value);
		}

		return this;
	}

	@NonNull
	@Override
	public URI toUri() {
		return builder.build().toUri();
	}

	@NonNull
	@Override
	public Link rel(@NonNull LinkRelation rel) {
		return new Link(rel, builder.toUriString(), method.name(), title, type, name, deprecation);
	}

	@NonNull
	@Override
	public LinkBuilder method(@NonNull HttpMethod method) {
		this.method = method;
		return this;
	}

	@NonNull
	@Override
	public LinkBuilder title(@Nullable String title) {
		this.title = title;
		return this;
	}

	@NonNull
	@Override
	public LinkBuilder type(@Nullable String type) {
		this.type = type;
		return this;
	}

	@NonNull
	@Override
	public LinkBuilder name(@Nullable String name) {
		this.name = name;
		return this;
	}

	@NonNull
	@Override
	public LinkBuilder deprecation(@Nullable String deprecation) {
		this.deprecation = deprecation;
		return this;
	}

	private static @Nullable String extractObjectValue(@Nullable Object object) {
		String value;

		if (object instanceof Optional<?> optional) {
			value = optional.map(Objects::toString).orElse(null);
		} else {
			value = Objects.toString(object, null);
		}

		return StringUtils.isBlank(value) ? null : value;
	}

	private UriComponentsBuilder createUriComponentsBuilder() {
		if (RequestContextHolder.getRequestAttributes() == null) {
			return UriComponentsBuilder.fromPath("/");
		}
		return ServletUriComponentsBuilder.fromCurrentServletMapping();
	}
}
