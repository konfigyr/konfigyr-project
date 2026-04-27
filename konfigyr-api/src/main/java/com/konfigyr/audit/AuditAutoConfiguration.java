package com.konfigyr.audit;

import com.konfigyr.entity.EntityId;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.util.Assert;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Spring autoconfiguration for the Konfigyr audit module.
 * <p>
 * Registers the {@link AuditEventRepository} responsible for storing and retrieving audit events,
 * and the centralized {@link AuditEventListener} that captures domain events from all bounded contexts
 * and persists them as audit entries.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class AuditAutoConfiguration {

	private final DSLContext context;

	@Bean
	AuditEventRepository auditEventRepository(ResourceLoader resourceLoader, JsonMapper jsonMapper) {
		final ClassLoader classLoader = resourceLoader.getClassLoader();
		Assert.notNull(classLoader, "Class loader can not be resolved");

		final BasicPolymorphicTypeValidator.Builder validator = BasicPolymorphicTypeValidator.builder()
				.allowIfSubType(EntityId.class);

		final JsonMapper auditJsonMapper = jsonMapper.rebuild()
				.addModules(SecurityJacksonModules.getModules(classLoader, validator))
				.build();

		return new AuditEventRepository(context, auditJsonMapper);
	}

	@Bean
	AuditEventListener.NamespaceResolver auditEventListenerNamespaceResolver() {
		return new AuditEventListener.NamespaceResolver(context);
	}

	@Bean
	AuditEventListener auditEventListener(AuditEventListener.NamespaceResolver resolver, AuditEventRepository repository) {
		return new AuditEventListener(resolver, repository);
	}

}
