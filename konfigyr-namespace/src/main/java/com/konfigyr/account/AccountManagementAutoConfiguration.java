package com.konfigyr.account;

import com.konfigyr.mail.Mailer;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
@AutoConfigureAfter(value = JooqAutoConfiguration.class, name = "com.konfigyr.mail.JavaMailerAutoConfiguration")
public class AccountManagementAutoConfiguration {

	private final ApplicationEventPublisher applicationEventPublisher;

	@Bean
	@ConditionalOnMissingBean(AccountManager.class)
	AccountManager defaultAccountManager(DSLContext context) {
		return new DefaultAccountManager(context, applicationEventPublisher);
	}

	@Bean
	@ConditionalOnBean(Mailer.class)
	AccountEventListener accountEventListener(Mailer mailer, AccountManager manager) {
		return new AccountEventListener(mailer, manager);
	}

}
