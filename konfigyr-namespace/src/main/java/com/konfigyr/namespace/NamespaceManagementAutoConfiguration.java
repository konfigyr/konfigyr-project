package com.konfigyr.namespace;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Spring autoconfiguration class for <code>konfigyr-namespace</code> module.
 *
 * @author Vladimir Spasic
 **/
@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class NamespaceManagementAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(NamespaceManager.class)
	NamespaceManager defaultNamespaceManager(DSLContext context) {
		return new DefaultNamespaceManager(context);
	}

}
