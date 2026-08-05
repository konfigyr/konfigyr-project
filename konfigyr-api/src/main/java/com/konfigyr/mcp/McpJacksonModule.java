package com.konfigyr.mcp;

import com.fasterxml.jackson.annotation.JsonValue;
import com.konfigyr.markdown.MarkdownContents;
import tools.jackson.databind.module.SimpleModule;

class McpJacksonModule extends SimpleModule {

	McpJacksonModule() {
		super("mcp-module");
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);

		context.setMixIn(MarkdownContents.class, MarkdownContentsMixin.class);
	}

	interface MarkdownContentsMixin {

		@JsonValue
		String value();

	}

}
