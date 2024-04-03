package com.konfigyr;

import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vladimir Spasic
 **/
@TestProfile
@EnableAutoConfiguration
@ImportTestcontainers(TestContainers.class)
@Configuration(proxyBeanMethods = false)
public class NamespaceTestConfiguration {
}
