package com.konfigyr.jooq;

import liquibase.exception.ChangeLogParseException;
import liquibase.exception.CommandExecutionException;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

class KonfigyrDatabaseTest {

	@Test
	@DisplayName("should start PostgreSQL Testcontainer and execute Liqiubase migrations")
	void shouldStartContainerAndExecuteMigrations() throws IOException {
		try (var database = createDatabase("/migrations")) {
			final var context = database.create();

			final var results = context.select(DSL.field("name"))
					.from("accounts")
					.fetch();

			assertThat(results)
					.isNotNull()
					.hasSize(1)
					.extracting(it -> it.getValue("name"))
					.containsExactly("John Doe");
		}
	}

	@Test
	@DisplayName("should fail to execute Liqiubase migrations when changelog is not found")
	void shouldFailToFindChangeLogFile() throws IOException {
		try (var database = createDatabase("/")) {
			assertThatThrownBy(database::create)
					.isInstanceOf(RuntimeException.class)
					.hasCauseInstanceOf(CommandExecutionException.class)
					.hasRootCauseInstanceOf(ChangeLogParseException.class);
		}
	}

	static KonfigyrDatabase createDatabase(String changelogs) throws IOException {
		final Resource resource = new ClassPathResource(changelogs);

		return new KonfigyrDatabase(resource.getFile());
	}

}