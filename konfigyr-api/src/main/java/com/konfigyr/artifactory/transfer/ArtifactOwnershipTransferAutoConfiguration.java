package com.konfigyr.artifactory.transfer;

import com.konfigyr.artifactory.ArtifactoryAutoConfiguration;
import com.konfigyr.artifactory.ownership.GroupVerifications;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter({ JooqAutoConfiguration.class, ArtifactoryAutoConfiguration.class })
public class ArtifactOwnershipTransferAutoConfiguration {

	private final DSLContext context;
	private final ApplicationEventPublisher eventPublisher;

	@Bean
	@ConditionalOnMissingBean(ArtifactOwnershipTransfers.class)
	ArtifactOwnershipTransfers defaultArtifactOwnershipTransfers(GroupVerifications groupVerifications) {
		return new DefaultArtifactOwnershipTransfers(context, groupVerifications, eventPublisher);
	}

}
