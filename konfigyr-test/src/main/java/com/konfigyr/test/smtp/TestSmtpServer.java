package com.konfigyr.test.smtp;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(GreenMailConfiguration.class)
public @interface TestSmtpServer {
}
