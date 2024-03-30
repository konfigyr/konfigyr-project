package com.konfigyr;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vladimir Spasic
 **/
@EnableAutoConfiguration
@Configuration(proxyBeanMethods = false)
public class NamespaceTestConfiguration {
}
