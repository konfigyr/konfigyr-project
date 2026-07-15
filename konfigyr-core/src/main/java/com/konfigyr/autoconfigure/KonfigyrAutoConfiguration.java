package com.konfigyr.autoconfigure;

import com.konfigyr.Hostnames;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Autoconfiguration used to register the {@link Hostnames} Spring configuration properties.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@AutoConfiguration
@EnableConfigurationProperties(Hostnames.class)
public class KonfigyrAutoConfiguration {

}
