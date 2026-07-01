import com.github.gradle.node.npm.task.NpmTask
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.node.gradle)
}

val ci = extra["ci"] as Provider<Boolean>

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

    implementation("org.bouncycastle:bcprov-jdk18on")

    implementation("org.liquibase:liquibase-core")
    implementation("org.postgresql:postgresql")

    developmentOnly(platform(libs.spring.boot.bom))
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation(project(":konfigyr-test"))
    testImplementation("org.seleniumhq.selenium:htmlunit3-driver")
    testImplementation("org.wiremock.integrations:wiremock-spring-boot")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

springBoot {
    buildInfo()
}

node {
    npmCommand.set(providers.gradleProperty("node.npm").get())
    npmInstallCommand.set(ci.map { if (it) "ci" else "install" })
}

tasks.register<NpmTask>("rollup") {
    dependsOn(tasks.named("npmInstall"))

    args.set(listOf("run", "build"))

    inputs.files("package.json", "package-lock.json", "rollup.config.js")
    inputs.dir("src/main/resources/assets")

    outputs.cacheIf { true }

    outputs.dir(project.layout.buildDirectory.dir("resources/main/static"))
}

tasks.register<NpmTask>("npmTest") {
    dependsOn(tasks.named("npmInstall"))

    args.set(listOf("test"))

    inputs.files("package.json", "package-lock.json", ".eslintrc.json", ".prettierrc.json")
    inputs.dir("src/main/assets/scripts")
}

tasks.named("processResources") { dependsOn("rollup") }

tasks.named<BootBuildImage>("bootBuildImage") {
    val dockerImageTag = project.extra["dockerImageTag"] as Provider<String>
    imageName.set(dockerImageTag)

    docker {
        publishRegistry {
            username.set("konfigyr")
            password.set(providers.environmentVariable("DOCKER_HUB_TOKEN").orElse(""))
        }
    }
}
