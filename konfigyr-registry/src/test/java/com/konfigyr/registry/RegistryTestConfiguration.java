package com.konfigyr.registry;

import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Test configuration used to test the Registry module.
 *
 * @author Vladimir Spasic
 **/
@TestProfile
@ComponentScan
@EnableAutoConfiguration
@ImportTestcontainers(TestContainers.class)
@Configuration(proxyBeanMethods = false)
public class RegistryTestConfiguration {

}
