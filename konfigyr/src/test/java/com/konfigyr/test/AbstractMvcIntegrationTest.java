package com.konfigyr.test;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Abstract test class for integration tests that use {@link MockMvc}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see AbstractIntegrationTest
 */
@AutoConfigureMockMvc
public abstract class AbstractMvcIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	protected MockMvcTester mvc;

	@LocalServerPort
	protected int localServerPort;

}
