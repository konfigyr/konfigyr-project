package com.konfigyr.observation;

import io.micrometer.common.annotation.ValueExpressionResolver;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.util.Assert;

@NullMarked
final class KonfigyrValueExpressionResolver implements ValueExpressionResolver {

	private final ExpressionParser expressionParser;
	private final EvaluationContext evaluationContext;

	KonfigyrValueExpressionResolver(ConversionService conversionService) {
		this(new SpelExpressionParser(), conversionService);
	}

	KonfigyrValueExpressionResolver(ExpressionParser expressionParser, ConversionService conversionService) {
		this.expressionParser = expressionParser;
		this.evaluationContext = SimpleEvaluationContext
				.forReadOnlyDataBinding()
				.withConversionService(conversionService)
				.build();
	}

	@Override
	public String resolve(String expression, @Nullable Object parameter) {
		final String value;

		try {
			final Expression expressionToEvaluate = expressionParser.parseExpression(expression);
			value = expressionToEvaluate.getValue(evaluationContext, parameter, String.class);
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to evaluate SpEL expression '%s'".formatted(expression), ex);
		}

		Assert.state(value != null, () -> "Value from '%s' expression must not be null".formatted(expression));
		return value;
	}
}
