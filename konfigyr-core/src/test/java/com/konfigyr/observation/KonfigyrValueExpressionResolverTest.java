package com.konfigyr.observation;

import io.micrometer.common.annotation.ValueExpressionResolver;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.format.support.FormattingConversionService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class KonfigyrValueExpressionResolverTest {

	FormattingConversionService conversions;
	ValueExpressionResolver resolver;

	@BeforeEach
	void setup() {
		conversions = new FormattingConversionService();
		ApplicationConversionService.configure(conversions);

		resolver = new KonfigyrValueExpressionResolver(conversions);
	}

	@Test
	@DisplayName("should evaluate valid expression to string")
	void checkValidExpression() {
		var value = Map.of("foo", Pair.of(1, 2));

		assertThat(resolver.resolve("['foo'].left", value))
				.isEqualTo("1");
	}

	@Test
	@DisplayName("should evaluate expression to string using conversion service")
	void checkConvertedExpression() {
		var value = Pair.of(1, 2);

		conversions.addConverter(Pair.class, String.class, (source) -> "Pair(%s,%s)".formatted(source.getLeft(), source.getRight()));

		assertThat(resolver.resolve("#this", value))
				.isEqualTo("Pair(1,2)");
	}

	@Test
	@DisplayName("should fail to evaluate invalid expression")
	void checkInvalidExpression() {
		var value = Map.of("foo", Pair.of(1, 2));
		assertThatIllegalStateException()
				.isThrownBy(() -> resolver.resolve("['bar'].right", value))
				.withCauseInstanceOf(SpelEvaluationException.class);
	}

	@Test
	@DisplayName("should fail to evaluate null expressions")
	void checkNullExpressions() {
		var value = Pair.of(null, null);

		assertThatIllegalStateException()
				.isThrownBy(() -> resolver.resolve("#this.right", value))
				.withMessageContaining("Value from '%s' expression must not be null", "#this.right")
				.withNoCause();
	}

	@Test
	@DisplayName("should fail to invoke methods via expression")
	void checkExpressionMethodAccess() {
		var value = Pair.of(1, 2);

		assertThatIllegalStateException()
				.isThrownBy(() -> resolver.resolve("toString()", value))
				.withCauseInstanceOf(SpelEvaluationException.class)
				.extracting(Throwable::getCause, InstanceOfAssertFactories.throwable(SpelEvaluationException.class))
				.returns(SpelMessage.METHOD_NOT_FOUND, SpelEvaluationException::getMessageCode);
	}

}
