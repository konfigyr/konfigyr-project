plugins {
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-cache")
    api("org.springframework.boot:spring-boot-starter-webmvc")
    api("org.springframework.boot:spring-boot-starter-jackson")
    api("org.springframework.boot:spring-boot-starter-validation")

    api("org.springframework.security:spring-security-web")
    api("org.springframework.security:spring-security-oauth2-core")

    api("org.springframework.modulith:spring-modulith-starter-core")
    api("org.jmolecules:jmolecules-ddd")
    api("org.jmolecules:jmolecules-events")

    api("com.konfigyr:konfigyr-crypto-api")
    api("com.konfigyr:konfigyr-crypto-tink")
    api("com.google.crypto.tink:tink:1.22.0")
    implementation("com.konfigyr:konfigyr-crypto-jdbc")

    api("com.github.slugify:slugify:4.0.0")
    implementation("com.ibm.icu:icu4j:78.3")

    api("io.hypersistence:hypersistence-tsid:2.1.4")

    api("commons-codec:commons-codec")

    testImplementation(project(":konfigyr-test"))

    testImplementation("org.liquibase:liquibase-core")
    testImplementation("org.postgresql:postgresql")

    testImplementation("org.bouncycastle:bcpkix-jdk18on")
}
