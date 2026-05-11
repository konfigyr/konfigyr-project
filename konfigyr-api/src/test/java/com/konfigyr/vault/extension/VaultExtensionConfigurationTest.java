package com.konfigyr.vault.extension;

import com.konfigyr.vault.VaultExtension;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VaultExtensionConfigurationTest {

	@Mock
	ApplicationEventPublisher applicationEventPublisher;

	ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

	@Test
	@DisplayName("should declare vault extension beans in correct order")
	void extensionSanityTest() {
		new ApplicationContextRunner()
				.withBean(ApplicationEventPublisher.class, () -> applicationEventPublisher)
				.withBean(ObservationRegistry.class, () -> observationRegistry)
				.withUserConfiguration(VaultExtensionConfiguration.class)
				.run(context -> {
					assertThat(context)
							.hasNotFailed();

					assertThat(context.getBeanProvider(VaultExtension.class).orderedStream())
							.map(Object::getClass)
							.map(Class.class::cast)
							.containsExactly(
									ObservedVaultExtension.class,
									LockingVaultExtension.class,
									PublishingVaultExtension.class
							);
				});
	}
}
