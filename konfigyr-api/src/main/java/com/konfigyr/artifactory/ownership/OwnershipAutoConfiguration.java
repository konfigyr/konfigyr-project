package com.konfigyr.artifactory.ownership;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class OwnershipAutoConfiguration {
	private final DSLContext context;

	@Bean
	@ConditionalOnMissingBean(GroupVerifications.class)
	GroupVerifications defaultGroupVerifications(VerificationStrategies verificationStrategies) {
		return new DefaultGroupVerifications(context, verificationStrategies);
	}

	@Bean
	DnsTxtVerificationStrategy dnsTxtVerificationStrategy() {
		return new DnsTxtVerificationStrategy();
	}

	@Bean
	SourceCodeVerificationStrategy sourceCodeVerificationStrategy(RestClient.Builder builder) {
		return new SourceCodeVerificationStrategy(builder.build());
	}

	@Bean
	VerificationStrategies verificationStrategies(ObjectProvider<VerificationStrategy> strategies) {
		return new VerificationStrategies(strategies);
	}
}
