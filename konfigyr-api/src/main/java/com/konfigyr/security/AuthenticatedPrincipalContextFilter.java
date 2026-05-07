package com.konfigyr.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@NullMarked
class AuthenticatedPrincipalContextFilter extends OncePerRequestFilter {

	static final String ACTOR_KEY = "actor";
	static final String ACTOR_TYPE_KEY = "actor.type";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
		try {
			AuthenticatedPrincipal.fromSecurityContext().ifPresent(principal -> {
				MDC.put(ACTOR_KEY, principal.get());
				MDC.put(ACTOR_TYPE_KEY, principal.getType().name());
			});

			chain.doFilter(request, response);
		} finally {
			MDC.remove(ACTOR_KEY);
			MDC.remove(ACTOR_TYPE_KEY);
		}
	}

}
