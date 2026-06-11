package com.konfigyr.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@AutoConfigureMockMvc
public abstract class AbstractControllerIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	protected MockMvcTester mvc;

}
