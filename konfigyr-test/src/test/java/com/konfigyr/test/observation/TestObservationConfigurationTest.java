package com.konfigyr.test.observation;

import com.konfigyr.test.smtp.TestSmtpServer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;

@TestSmtpServer
@TestObservations
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = TestObservationConfigurationTest.class)
class TestObservationConfigurationTest {

	@Autowired
	ObservationRegistry registry;

	@Test
	@DisplayName("should register a test implementation of the Observation registry")
	void registeredTestInstance() {
		assertThatObject(registry)
				.isNotNull()
				.isInstanceOf(TestObservationRegistry.class);
	}

	@Test
	@Order(1)
	@DisplayName("should record first observation in the test Observation registry")
	void shouldRecordFirstObservation() {
		Observation.start("first.observation", registry).stop();

		assertThat((TestObservationRegistry) registry)
				.hasObservationWithNameEqualTo("first.observation")
				.that()
				.hasBeenStopped();
	}

	@Test
	@Order(2)
	@DisplayName("should verify that the first observation has been removed from test Observation registry")
	void shouldResetObservationRegistry() {
		assertThat((TestObservationRegistry) registry)
				.doesNotHaveAnyObservation();
	}

}
