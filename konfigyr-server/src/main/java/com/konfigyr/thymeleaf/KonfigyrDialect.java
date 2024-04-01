package com.konfigyr.thymeleaf;

import org.springframework.core.env.Environment;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.StandardDialect;

import java.util.Set;

/**
 * Konfigyr Thymelaf {@link AbstractProcessorDialect} implementation.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
class KonfigyrDialect extends AbstractProcessorDialect {

	static final String DIALECT_NAME = "KonfigyrDialect";

	/**
	 * The Konfigyr dialect prefix, used like this: <code>konfigyr:*</code>.
	 */
	static final String DIALECT_PREFIX = "konfigyr";

	private final Environment environment;

	KonfigyrDialect(Environment environment) {
		super(DIALECT_NAME, DIALECT_PREFIX, StandardDialect.PROCESSOR_PRECEDENCE);
		this.environment = environment;
	}

	@Override
	public Set<IProcessor> getProcessors(String dialectPrefix) {
		return Set.of(new TestSelectorProcessor(dialectPrefix, environment));
	}
}
