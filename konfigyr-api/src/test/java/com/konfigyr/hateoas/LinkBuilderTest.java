package com.konfigyr.hateoas;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class LinkBuilderTest {

	protected MockHttpServletRequest request;

	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
	}

	@AfterEach
	void cleanup() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	@DisplayName("creates a link using request information from request attributes")
	void createsLinkWithRequestAttributes() {
		Link link = Link.builder()
				.path("namespaces")
				.path("konfigyr")
				.query("foo", "bar", "", null, "baz", "  ")
				.query("bar", (List<Object>) null)
				.title("Konfigyr")
				.selfRel();

		assertThat(link)
				.isNotNull()
				.returns(LinkRelation.SELF, Link::rel)
				.returns("http://localhost/namespaces/konfigyr?foo=bar&foo=baz", Link::href)
				.returns("Konfigyr", Link::title)
				.returns(null, Link::type)
				.returns(null, Link::name)
				.returns(null, Link::deprecation);
	}

	@Test
	@DisplayName("creates a link without request attribute context")
	void createsLinkWithoutRequestAttributes() {
		RequestContextHolder.resetRequestAttributes();

		Link link = Link.builder()
				.path("/namespaces/konfigyr")
				.path("/members")
				.path(12745)
				.title("title")
				.type("type")
				.name("name")
				.deprecation("deprecation")
				.rel("related");

		assertThat(link)
				.isNotNull()
				.returns(LinkRelation.RELATED, Link::rel)
				.returns("/namespaces/konfigyr/members/12745", Link::href)
				.returns("title", Link::title)
				.returns("type", Link::type)
				.returns("name", Link::name)
				.returns("deprecation", Link::deprecation);
	}

	@Test
	@DisplayName("link builder should check slash object when creating URI")
	void createsUri() {
		LinkBuilder builder = Link.builder();

		assertThat(builder.toUri())
				.isEqualTo(URI.create("http://localhost"));

		assertThat(builder.path(null).toUri())
				.isEqualTo(URI.create("http://localhost"));

		assertThat(builder.path("").toUri())
				.isEqualTo(URI.create("http://localhost"));

		assertThat(builder.path("  ").toUri())
				.isEqualTo(URI.create("http://localhost"));

		assertThat(builder.path(Optional.empty()).toUri())
				.isEqualTo(URI.create("http://localhost"));

		assertThat(builder.path(Optional.of("  ")).toUri())
				.isEqualTo(URI.create("http://localhost"));

		assertThat(builder.path("#").toUri())
				.isEqualTo(URI.create("http://localhost"));

		assertThat(builder.path("api#").path(12345).path(true).toUri())
				.isEqualTo(URI.create("http://localhost/api/12345/true"));
	}

	@Test
	@DisplayName("use `X-Forwarded-Host` when creating links")
	void usesForwardedHostAsHostIfHeaderIsSet() throws Exception {
		request.addHeader("X-Forwarded-Host", "forwarded-host");

		useForwardedHeaders();

		Link link = Link.builder().path("/namespaces").selfRel();

		assertThat(link.href())
				.isEqualTo("http://forwarded-host/namespaces");
	}

	@Test
	@DisplayName("use enabled `X-Forwarded-Ssl` when creating links")
	void usesForwardedSslIfHeaderIsEnabled() throws Exception {
		request.addHeader("X-Forwarded-Ssl", "on");

		useForwardedHeaders();

		Link link = Link.builder().path("/namespaces").selfRel();

		assertThat(link.href())
				.isEqualTo("https://localhost/namespaces");
	}

	@Test
	@DisplayName("use disabled `X-Forwarded-Ssl` when creating links")
	void usesForwardedSslIfHeaderIsDisabled() throws Exception {
		request.addHeader("X-Forwarded-Ssl", "off");

		useForwardedHeaders();

		Link link = Link.builder().path("/namespaces").selfRel();

		assertThat(link.href())
				.isEqualTo("http://localhost/namespaces");
	}

	@Test
	@DisplayName("use all available combinations of `X-Forwarded` headers when creating links")
	void usesForwardedHeaders() throws Exception {
		request.addHeader("X-Forwarded-Host", "secure-host");
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("X-Forwarded-Port", "8443");
		request.addHeader("X-Forwarded-For", "172.0.0.1");

		useForwardedHeaders();

		Link link = Link.builder().path("/namespaces").selfRel();

		assertThat(link.href())
				.isEqualTo("https://secure-host:8443/namespaces");
	}

	protected void useForwardedHeaders() throws Exception {
		final var chain = new MockFilterChain();
		new ForwardedHeaderFilter().doFilter(request, new MockHttpServletResponse(), chain);

		HttpServletRequest proxied = (HttpServletRequest) chain.getRequest();
		assertThat(proxied)
				.isNotNull();

		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(proxied));
	}

}
