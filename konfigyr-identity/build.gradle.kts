import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    java
    alias(libs.plugins.spring.boot)
}

val extension = the<KonfigyrBuildExtension>()

dependencies {
    implementation(project(":konfigyr-core"))
    implementation(project(":konfigyr-data"))
    implementation(project(":konfigyr-mail"))

    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")

    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("io.micrometer:micrometer-observation")

    implementation("org.springframework.retry:spring-retry")

    implementation("org.springframework.session:spring-session-jdbc")

    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")

    implementation("com.konfigyr:konfigyr-crypto-jose")

    implementation(libs.bouncy.castle.provider)

    implementation("org.liquibase:liquibase-core")
    implementation("org.postgresql:postgresql")

    developmentOnly(platform(libs.spring.boot.bom))
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation(project(":konfigyr-test"))
    testImplementation("org.seleniumhq.selenium:htmlunit3-driver")
    testImplementation(libs.wiremock)
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

springBoot {
    buildInfo()
}

tasks.register<NpmExec>("npmInstall") {
    description = "Installs the required NPM dependencies"
    group = "build"

    args.set(extension.ci.map { if (it) listOf("ci") else listOf("install") })
    sources.from("package.json", "package-lock.json")
    outputFile.set(layout.buildDirectory.file("npm-install.stamp"))

    // node_modules is not declared as an output so the stamp alone cannot
    // serve as a valid cache entry — restoring it without restoring node_modules
    // would break every downstream task.
    outputs.cacheIf { false }
}

tasks.register<NpmExec>("rollup") {
    dependsOn("npmInstall")
    description = "Bundles static assets for the identity server UI"
    group = "build"

    args.set(listOf("run", "build"))
    sources.from(
        "package.json", "package-lock.json", "rollup.config.js",
        fileTree("src/main/resources/assets")
    )
    outputDir.set(layout.buildDirectory.dir("resources/main/static"))
}

tasks.named("processResources") { dependsOn("rollup") }

tasks.named<BootBuildImage>("bootBuildImage") {
    imageName.set(extension.dockerImageTag)

    docker {
        publishRegistry {
            username.set("konfigyr")
            password.set(providers.environmentVariable("DOCKER_HUB_TOKEN"))
        }
    }
}
