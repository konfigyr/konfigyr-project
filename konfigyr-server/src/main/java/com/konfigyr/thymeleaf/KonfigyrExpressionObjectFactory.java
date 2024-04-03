package com.konfigyr.thymeleaf;

import org.springframework.util.function.SingletonSupplier;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Implementation of the {@link IExpressionObjectFactory} that allows contributing helper
 * object expressions to Thymeleaf.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
class KonfigyrExpressionObjectFactory implements IExpressionObjectFactory {

	static final Set<String> ALL_EXPRESSION_OBJECT_NAMES;

	private static final Supplier<IExpressionObjectFactory> instance = SingletonSupplier
			.of(KonfigyrExpressionObjectFactory::new);

	static {
		final Set<String> allExpressionObjectNames = new LinkedHashSet<>();
		allExpressionObjectNames.add(Forms.OBJECT_NAME);

		ALL_EXPRESSION_OBJECT_NAMES = Collections.unmodifiableSet(allExpressionObjectNames);
	}

	public static IExpressionObjectFactory getInstance() {
		return instance.get();
	}

	@Override
	public Set<String> getAllExpressionObjectNames() {
		return ALL_EXPRESSION_OBJECT_NAMES;
	}

	@Override
	public boolean isCacheable(String expressionObjectName) {
		return true;
	}

	@Override
	public Object buildObject(IExpressionContext context, String expressionObjectName) {
		if (Forms.OBJECT_NAME.equals(expressionObjectName)) {
			return new Forms(context);
		}
		return null;
	}
}
