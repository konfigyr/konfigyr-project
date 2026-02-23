package com.konfigyr.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Order;
import org.springframework.boot.system.ApplicationTemp;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.io.File;
import java.io.IOException;

@Slf4j
@NullMarked
@Order(Ordered.HIGHEST_PRECEDENCE)
final class TemporaryDirectoryContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	final ApplicationTemp temp = new ApplicationTemp(TemporaryDirectoryContextInitializer.class);

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		final File temporaryDirectory = temp.getDir("konfigyr-test");

		log.info("Creating temporary directory: {}", temporaryDirectory);

		applicationContext.addApplicationListener(new TemporaryDirectoryCleaner(temporaryDirectory));

		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
					"konfigyr.artifactory.metadata-store.root=" + temporaryDirectory.toURI());
	}

	@RequiredArgsConstructor
	static final class TemporaryDirectoryCleaner implements ApplicationListener<ContextClosedEvent> {

		private final File temporaryDirectory;

		@Override
		public void onApplicationEvent(ContextClosedEvent event) {
			log.info("Deleting temporary directory: {}", temporaryDirectory);

			try {
				FileUtils.deleteDirectory(temporaryDirectory);
			} catch (IOException ex) {
				throw new IllegalStateException("Unable to delete temporary directory: " + temporaryDirectory, ex);
			}
		}
	}

}
