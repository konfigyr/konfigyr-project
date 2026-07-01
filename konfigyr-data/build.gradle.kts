import nu.studer.gradle.jooq.JooqGenerate
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Generate
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Strategy
import org.jooq.meta.jaxb.Target

plugins {
    `java-library`
    alias(libs.plugins.studer.jooq)
}

description = "Konfigyr module that contains jOOQ generated classes and database migrations"

dependencies {
    api(project(":konfigyr-core"))

    api("org.springframework.boot:spring-boot-starter-jooq")
    api("org.springframework.boot:spring-boot-starter-liquibase")
    api("org.springframework.data:spring-data-commons")
    compileOnly("org.springframework.boot:spring-boot-starter-batch")

    implementation("org.liquibase:liquibase-core")
    implementation("org.postgresql:postgresql")

    jooqGenerator(project(":konfigyr-jooq-extensions"))
    jooqGenerator("org.springframework.boot:spring-boot-starter-batch")
    jooqGenerator("ch.qos.logback:logback-classic")

    testImplementation(project(":konfigyr-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase")
}

jooq {
    version.set(libs.versions.jooq.get())

    configurations {
        create("main") {
            jooqConfiguration.apply {
                generator = Generator().apply {
                    name = "org.jooq.codegen.JavaGenerator"
                    database = Database().apply {
                        name = "com.konfigyr.jooq.KonfigyrDatabase"
                        inputSchema = "public"
                        excludes = "databasechangelog.*,vault_property_history_*,vault_change_history_*,audit_events_*"
                        forcedTypes.add(ForcedType().apply {
                            userType = "com.konfigyr.io.ByteArray"
                            converter = "com.konfigyr.data.converter.ByteArrayConverter"
                            includeTypes = "bytea"
                        })
                    }
                    generate = Generate().apply {
                        isPojos = false
                        isRecords = false
                        isInterfaces = false
                        isJavaTimeTypes = true
                        isJavadoc = true
                    }
                    target = Target().apply {
                        packageName = "com.konfigyr.data"
                    }
                    strategy = Strategy().apply {
                        name = "org.jooq.codegen.DefaultGeneratorStrategy"
                    }
                }
            }
        }
    }
}

tasks.named<JooqGenerate>("generateJooq") {
    inputs.files(fileTree("src/main/resources/migrations"))
        .withPropertyName("migrations")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    allInputsDeclared.set(true)

    outputs.cacheIf { true }
}
