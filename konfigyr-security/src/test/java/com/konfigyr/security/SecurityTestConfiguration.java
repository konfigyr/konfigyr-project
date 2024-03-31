package com.konfigyr.security;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vladimir Spasic
 **/
@EnableCaching
@EnableAutoConfiguration
@Configuration(proxyBeanMethods = false)
public class SecurityTestConfiguration {
}
