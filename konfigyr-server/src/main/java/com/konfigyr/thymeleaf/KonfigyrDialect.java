package com.konfigyr.thymeleaf;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.Set;

/**
 * Konfigyr Thymelaf {@link IProcessorDialect} implementation.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Component
public class KonfigyrDialect extends AbstractDialect implements IProcessorDialect {

	private static final String NAME = "konfigyr";

	private final Environment environment;

	protected KonfigyrDialect(Environment environment) {
		super(NAME);
		this.environment = environment;
	}

	@Override
	public String getPrefix() {
		return NAME;
	}

	@Override
	public int getDialectProcessorPrecedence() {
		return 1000;
	}

	@Override
	public Set<IProcessor> getProcessors(String dialectPrefix) {
		return Set.of(new TestSelectorProcessor(dialectPrefix, environment));
	}
}
