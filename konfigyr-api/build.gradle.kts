import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    java
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":konfigyr-core"))
    implementation(project(":konfigyr-data"))
    implementation(project(":konfigyr-mail"))

    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-batch-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-restclient")

    implementation(libs.konfigyr.artifactory)

    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("io.micrometer:micrometer-observation")

    implementation("org.springframework.retry:spring-retry")

    implementation(libs.commonmark.core)
    implementation(libs.commonmark.autolink)
    implementation(libs.commonmark.strikethrough)
    implementation(libs.commonmark.tasklist)
    implementation(libs.owasp.sanitizer)

    implementation(libs.bouncy.castle.provider)

    implementation(libs.jgit)

    implementation("org.liquibase:liquibase-core")
    implementation("org.postgresql:postgresql")

    developmentOnly(platform(libs.spring.boot.bom))
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation(project(":konfigyr-test"))
    testImplementation(libs.greenmail)
    testImplementation(libs.wiremock)
    testImplementation("org.springframework.boot:spring-boot-starter-batch-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-micrometer-metrics-test")
}

springBoot {
    buildInfo()
}

tasks.named<BootBuildImage>("bootBuildImage") {
    imageName.set(the<KonfigyrBuildExtension>().dockerImageTag)

    docker {
        publishRegistry {
            username.set("konfigyr")
            password.set(providers.environmentVariable("DOCKER_HUB_TOKEN"))
        }
    }
}
