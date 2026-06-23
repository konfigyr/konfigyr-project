package com.konfigyr.artifactory.ownership;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class GroupVerificationAutoConfiguration {
	private final DSLContext context;

	@Bean
	@ConditionalOnMissingBean(GroupVerifications.class)
	GroupVerifications defaultGroupVerifications(DnsTxtVerificationStrategy dnsTxtVerificationStrategy,
			SourceCodeVerificationStrategy sourceCodeVerificationStrategy) {
		return new DefaultGroupVerifications(context, dnsTxtVerificationStrategy, sourceCodeVerificationStrategy);
	}

	@Bean
	DnsTxtVerificationStrategy dnsTxtVerificationStrategy() {
		return new DnsTxtVerificationStrategy();
	}

	@Bean
	SourceCodeVerificationStrategy sourceCodeVerificationStrategy(RestClient.Builder builder) {
		return new SourceCodeVerificationStrategy(builder.build());
	}
}
