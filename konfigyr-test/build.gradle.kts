plugins {
    `java-library`
}

dependencies {
    compileOnly(project(":konfigyr-core"))

    compileOnly("com.konfigyr:konfigyr-mail-api:1.0.0")
    compileOnly("com.icegreen:greenmail")

    compileOnly("org.springframework.boot:spring-boot-starter-webmvc-test")
    compileOnly("org.springframework.boot:spring-boot-starter-micrometer-metrics-test")

    implementation("org.springframework.boot:spring-boot-starter-validation")

    api("org.springframework.security:spring-security-test")

    compileOnly("org.springframework.security:spring-security-oauth2-core")

    api("org.springframework.modulith:spring-modulith-starter-core")
    api("org.springframework.modulith:spring-modulith-starter-test")

    api("org.jmolecules.integrations:jmolecules-starter-test")

    api("org.springframework.boot:spring-boot-jdbc")
    api("org.springframework.boot:spring-boot-testcontainers")
    api("org.testcontainers:testcontainers-junit-jupiter")
    api("org.testcontainers:testcontainers-postgresql")

    testImplementation(project(":konfigyr-core"))
    testImplementation("com.icegreen:greenmail")
    testImplementation("com.konfigyr:konfigyr-mail-api:1.0.0")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-oauth2-core")
    testImplementation("org.springframework.boot:spring-boot-starter-micrometer-metrics-test")
}

tasks.withType<Javadoc>().configureEach {
    isEnabled = false
}
