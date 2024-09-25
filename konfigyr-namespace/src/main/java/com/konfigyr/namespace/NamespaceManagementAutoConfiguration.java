package com.konfigyr.namespace;

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
 * Spring autoconfiguration class for <code>konfigyr-namespace</code> module.
 *
 * @author Vladimir Spasic
 **/
@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class NamespaceManagementAutoConfiguration {

	private final ApplicationEventPublisher applicationEventPublisher;

	@Bean
	@ConditionalOnMissingBean(Invitations.class)
	Invitations defaultInvitations(DSLContext context) {
		return new DefaultInvitations(context, applicationEventPublisher);
	}

	@Bean
	@ConditionalOnMissingBean(NamespaceManager.class)
	NamespaceManager defaultNamespaceManager(DSLContext context) {
		return new DefaultNamespaceManager(context, applicationEventPublisher);
	}

	@Bean
	@ConditionalOnBean(Mailer.class)
	InvitationSender invitationSender(Mailer mailer, DSLContext context) {
		return new InvitationSender(mailer, context);
	}
}
