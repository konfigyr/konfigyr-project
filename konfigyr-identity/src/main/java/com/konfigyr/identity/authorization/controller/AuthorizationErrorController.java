package com.konfigyr.identity.authorization.controller;

import com.konfigyr.identity.authorization.AuthorizationFailureHandler;
import com.konfigyr.web.error.OAuth2ErrorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@NullMarked
@Controller
@RequestMapping(AuthorizationFailureHandler.OAUTH_ERROR_PAGE)
class AuthorizationErrorController {

	private final OAuth2ErrorResolver resolver = OAuth2ErrorResolver.getInstance();
	private final HttpMessageConverter<OAuth2Error> converter = new OAuth2ErrorHttpMessageConverter();

	@RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
	ModelAndView html(Model model, HttpServletRequest request) {
		model.addAttribute("error", resolve(request));

		return new ModelAndView("oauth-error", model.asMap(), HttpStatus.BAD_REQUEST);
	}

	@RequestMapping
	void error(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setStatus(HttpStatus.BAD_REQUEST.value());

		converter.write(resolve(request), MediaType.APPLICATION_JSON, new ServletServerHttpResponse(response));
	}

	private OAuth2Error resolve(HttpServletRequest request) {
		OAuth2Error error = resolver.resolve(request);

		if (error == null) {
			error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR);
		}

		return error;
	}

}
