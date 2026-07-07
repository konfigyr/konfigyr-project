plugins {
    `java-library`
}

dependencies {
    implementation("org.liquibase:liquibase-core")
    implementation("org.postgresql:postgresql")
    implementation("org.testcontainers:testcontainers-postgresql")

    compileOnly("org.jooq:jooq-meta")
    compileOnly("org.jooq:jooq-codegen")

    testImplementation(project(":konfigyr-test"))
    testImplementation("org.jooq:jooq-meta")
    testImplementation("org.jooq:jooq-codegen")
    testImplementation("ch.qos.logback:logback-classic")
}
