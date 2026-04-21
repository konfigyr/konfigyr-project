package com.konfigyr.markdown;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureBefore(JacksonAutoConfiguration.class)
public class MarkdownAutoconfiguration {

	@Bean
	@ConditionalOnMissingBean(MarkdownParser.class)
	MarkdownParser commonmarkMarkdownParser() {
		return new CommonmarkMarkdownParser();
	}

	@Bean
	MarkdownModule markdownModule(MarkdownParser parser) {
		return new MarkdownModule(parser);
	}

}
