package com.konfigyr.membership;

import com.konfigyr.mail.Mailer;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.feature.Features;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * Spring autoconfiguration class for the {@code konfigyr-membership} module.
 *
 * @author Vladimir Spasic
 **/
@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class MembershipAutoConfiguration {

	private final DSLContext context;
	private final ApplicationEventPublisher applicationEventPublisher;

	@Bean
	@ConditionalOnMissingBean(Memberships.class)
	Memberships defaultMemberships() {
		return new DefaultMemberships(context, applicationEventPublisher);
	}

	@Bean
	@ConditionalOnMissingBean(Invitations.class)
	Invitations defaultInvitations(NamespaceManager namespaces, Features features) {
		return new DefaultInvitations(context, features, namespaces, applicationEventPublisher);
	}

	@Bean
	@ConditionalOnBean(Mailer.class)
	InvitationSender invitationSender(Mailer mailer) {
		return new InvitationSender(mailer, context);
	}

}
