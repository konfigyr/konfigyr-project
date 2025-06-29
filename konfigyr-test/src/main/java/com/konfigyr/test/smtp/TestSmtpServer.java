package com.konfigyr.test.smtp;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(GreenMailConfiguration.class)
@TestExecutionListeners(
		listeners = GreenMailConfiguration.GreenMailTestExecutionListener.class,
		mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
public @interface TestSmtpServer {
}
