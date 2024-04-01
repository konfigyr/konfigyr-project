package com.konfigyr.thymeleaf;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeDefinition;
import org.thymeleaf.engine.AttributeDefinitions;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.engine.IAttributeDefinitionsAware;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.processor.AbstractStandardExpressionAttributeTagProcessor;
import org.thymeleaf.standard.util.StandardProcessorUtils;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.util.EscapedAttributeUtils;
import org.thymeleaf.util.StringUtils;
import org.thymeleaf.util.Validate;

/**
 * Implementation of the {@link org.thymeleaf.processor.element.IElementTagProcessor} that would
 * append the <code>data-test</code> HTML attribute that would be used as a test selector for our
 * integration tests.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
class TestSelectorProcessor extends AbstractStandardExpressionAttributeTagProcessor implements IAttributeDefinitionsAware {

	private static final String ATTRIBUTE_NAME = "test-selector";
	private static final String TARGET_ATTRIBUTE_NAME = "data-" + ATTRIBUTE_NAME;
	private static final Profiles MATCHING_PROFILES = Profiles.of("test | local");

	private final Environment environment;
	private AttributeDefinition attributeDefinition;

	TestSelectorProcessor(String dialectPrefix, Environment environment) {
		super(TemplateMode.HTML, dialectPrefix, ATTRIBUTE_NAME, 1000, false);
		this.environment = environment;
	}

	@Override
	public void setAttributeDefinitions(AttributeDefinitions attributeDefinitions) {
		Validate.notNull(attributeDefinitions, "Attribute Definitions cannot be null");
		this.attributeDefinition = attributeDefinitions.forName(getTemplateMode(), TARGET_ATTRIBUTE_NAME);
	}

	@Override
	protected final void doProcess(
			ITemplateContext context, IProcessableElementTag tag, AttributeName name,
			String value, Object result, IElementTagStructureHandler handler
	) {
		if (!environment.acceptsProfiles(MATCHING_PROFILES)) {
			handler.removeAttribute(name);
			return;
		}

		final String escaped = EscapedAttributeUtils.escapeAttribute(getTemplateMode(), result == null ? null : result.toString());
		if (StringUtils.isEmptyOrWhitespace(escaped)) {
			handler.removeAttribute(name);
		} else {
			StandardProcessorUtils.replaceAttribute(handler, name, attributeDefinition, TARGET_ATTRIBUTE_NAME, escaped);
		}
	}
}
