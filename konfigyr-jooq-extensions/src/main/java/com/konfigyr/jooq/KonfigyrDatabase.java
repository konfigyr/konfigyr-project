package com.konfigyr.jooq;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.DirectoryResourceAccessor;
import org.jooq.DSLContext;
import org.jooq.meta.postgres.PostgresDatabase;
import org.jooq.tools.JooqLogger;
import org.jooq.tools.jdbc.JDBCUtils;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Customized JOOQ {@link PostgresDatabase} that would be used with Gradle
 * <a href="https://github.com/etiennestuder/gradle-jooq-plugin"><code>jooqGenerate</code></a>
 * plugin.
 * <p>
 * The goal of this database type is to start a {@link PostgreSQLContainer} and execute the
 * {@link Liquibase} changesets against it. Once the migrations are executed and the database
 * schema is updated, the JOOQ {@link org.jooq.codegen.Generator} would be executed.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see <a href="https://github.com/etiennestuder/gradle-jooq-plugin">Gradle JOOQ plugin</a>
 **/
public class KonfigyrDatabase extends PostgresDatabase {

	private static final String CHANGELOG = "src/main/resources/migrations";
	private static final String CHANGELOG_NAME = "changelog.xml";
	private static final String POSTGRESQL_IMAGE = "postgres:17.2-alpine";

	private final JooqLogger logger = JooqLogger.getLogger(KonfigyrDatabase.class);
	private final File changelogs;

	private PostgreSQLContainer<?> container;
	private Connection connection;

	/**
	 * Creates a new instance of the {@link KonfigyrDatabase} using the default Liquibase
	 * changelog folder that contains the <code>changelog.xml</code> master file.
	 */
	public KonfigyrDatabase() {
		this(Path.of(CHANGELOG).toFile());
	}

	KonfigyrDatabase(File changelogs) {
		this.changelogs = changelogs;
	}

	protected String getChangelog() {
		String changelog = getProperties().getProperty("changelog");

		if (changelog == null) {
			changelog = CHANGELOG_NAME;
		}

		return changelog;
	}

	@Override
	protected DSLContext create0() {
		if (connection == null) {
			startContainer();
			establishConnection();

			migrate();

			setConnection(connection);
		}

		return super.create0();
	}

	private void startContainer() {
		logger.info("Staring PostgreSQL container...");

		container = new PostgreSQLContainer<>(POSTGRESQL_IMAGE)
				.withDatabaseName("konfigyr");

		container.start();
	}

	private void establishConnection() {
		logger.info("Establishing JDBC connection to Postgresql container: " + container.getJdbcUrl());

		try {
			connection = DriverManager.getConnection(
					container.getJdbcUrl(),
					container.getUsername(),
					container.getPassword()
			);
		} catch (SQLException e) {
			throw new RuntimeException("Failed to establish JDBC connection to: " + container.getJdbcUrl(), e);
		}
	}

	@SuppressWarnings("deprecation")
	private void migrate() {
		logger.info("Executing Liquibase migrations from changeset: " + changelogs);

		final Liquibase liquibase;

		try {
			final Database database = DatabaseFactory.getInstance()
					.findCorrectDatabaseImplementation(new JdbcConnection(connection));

			database.setDefaultSchemaName("public");
			database.setDefaultCatalogName(container.getDatabaseName());

			liquibase = new Liquibase(
					getChangelog(),
					new CompositeResourceAccessor(
							new DirectoryResourceAccessor(changelogs)
					),
					database
			);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Failed to locate changeset file in location: " + changelogs, e);
		} catch (LiquibaseException e) {
			throw new RuntimeException("Failed to create Liquibase", e);
		}

		try {
			liquibase.update(new Contexts());
		} catch (LiquibaseException e) {
			throw new RuntimeException("Failed to run Liquibase migrations", e);
		}

		logger.info("Successfully executed Liquibase migrations");
	}

	@Override
	public void close() {
		JDBCUtils.safeClose(connection);
		JDBCUtils.safeClose(container);

		container = null;
		connection = null;

		super.close();
	}
}
