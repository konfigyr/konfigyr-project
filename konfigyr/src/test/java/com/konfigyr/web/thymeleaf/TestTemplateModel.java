package com.konfigyr.web.thymeleaf;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

/**
 * @author Vladimir Spasic
 **/
public record TestTemplateModel(
		@NotEmpty String title,
		@Email String email,
		@Min(19) int age,
		@AssertTrue boolean agreedOnTerms
) {
}
