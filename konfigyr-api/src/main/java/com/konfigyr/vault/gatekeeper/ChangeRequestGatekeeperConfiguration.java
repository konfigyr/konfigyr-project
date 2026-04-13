package com.konfigyr.vault.gatekeeper;

import com.konfigyr.namespace.Services;
import com.konfigyr.vault.ProfileManager;
import com.konfigyr.vault.state.StateRepositoryFactory;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class ChangeRequestGatekeeperConfiguration {

	@Bean
	GateContextFactory gateContextFactory(
			DSLContext context,
			Services services,
			ProfileManager profiles,
			StateRepositoryFactory stateRepositoryFactory
	) {
		return new GateContextFactory(context, services, profiles, stateRepositoryFactory);
	}

	@Bean
	ChangeRequestGatekeeper changeRequestGatekeeper(GateContextFactory contextFactory) {
		return new ChangeRequestGatekeeper(contextFactory, List.of(
				new LifecycleGate(),
				new RepositoryStateGate(),
				new ReviewStateGate()
		));
	}

}
