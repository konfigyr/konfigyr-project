package com.konfigyr.artifactory.ownership;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class GroupVerificationAutoConfiguration {
	private final DSLContext context;

	@Bean
	@ConditionalOnMissingBean(GroupVerifications.class)
	GroupVerifications defaultGroupVerifications() {
		return new DefaultGroupVerifications(context);
	}

	@Bean
	DnsTxtVerificationStrategy dnsTxtVerificationStrategy() {
		return new DnsTxtVerificationStrategy();
	}

	@Bean
	SourceCodeVerificationStrategy sourceCodeVerificationStrategy() {
		final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(10));
		factory.setReadTimeout(Duration.ofSeconds(20));

		return new SourceCodeVerificationStrategy(RestClient.builder()
				.requestFactory(factory)
				.build());
	}
}
