package com.konfigyr.account;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * Spring autoconfiguration class for <code>konfigyr-accounts</code> module.
 *
 * @author Vladimir Spasic
 **/
@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class NamespaceAccountsAutoConfiguration {

	private final ApplicationEventPublisher applicationEventPublisher;

	@Bean
	@ConditionalOnMissingBean(AccountManager.class)
	AccountManager defaultAccountManager(DSLContext context) {
		return new DefaultAccountManager(context, applicationEventPublisher);
	}

}
