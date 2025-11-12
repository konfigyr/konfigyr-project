package com.konfigyr.namespace;

import com.konfigyr.feature.FeatureDefinition;
import com.konfigyr.feature.FeatureDefinitionConfigurer;
import com.konfigyr.feature.Features;
import com.konfigyr.mail.Mailer;
import com.konfigyr.security.PasswordEncoders;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;

/**
 * Spring autoconfiguration class for <code>konfigyr-namespace</code> module.
 *
 * @author Vladimir Spasic
 **/
@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class NamespaceManagementAutoConfiguration implements FeatureDefinitionConfigurer {

	private final ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void configure(@NonNull Collection<FeatureDefinition<?>> definitions) {
		definitions.add(NamespaceFeatures.MEMBERS_COUNT);
		definitions.add(NamespaceFeatures.SERVICES_COUNT);
	}

	@Bean
	@ConditionalOnMissingBean(Invitations.class)
	Invitations defaultInvitations(DSLContext context, Features features) {
		return new DefaultInvitations(context, features, applicationEventPublisher);
	}

	@Bean
	@ConditionalOnMissingBean(NamespaceManager.class)
	NamespaceManager defaultNamespaceManager(DSLContext context, ObjectProvider<PasswordEncoder> passwordEncoder) {
		final PasswordEncoder encoder = passwordEncoder.getIfAvailable(PasswordEncoders::get);
		return new DefaultNamespaceManager(context, encoder, applicationEventPublisher);
	}

	@Bean
	@ConditionalOnBean(Mailer.class)
	InvitationSender invitationSender(Mailer mailer, DSLContext context) {
		return new InvitationSender(mailer, context);
	}

	@Bean
	@ConditionalOnMissingBean(Services.class)
	Services defaultNamespaceServices(DSLContext context, NamespaceManager namespaces) {
		return new DefaultServices(context, namespaces, applicationEventPublisher);
	}

}
