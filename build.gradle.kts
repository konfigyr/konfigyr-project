import io.freefair.gradle.plugins.lombok.LombokExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.lombok)      apply false
    alias(libs.plugins.spring.boot) apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

// Capture the catalog at root level so it is accessible as a closed-over variable
// inside subprojects {} without being resolved against each subproject's extensions.
val catalog = libs

subprojects {
    val projectName = project.name
    val projectVersion = project.version.toString()

    val ci = providers.environmentVariable("CI")
        .map { it.toBoolean() }
        .orElse(false)
    extra["ci"] = ci

    val nightly = providers.environmentVariable("NIGHTLY")
        .map { it.toBoolean() }
        .orElse(false)
    extra["nightly"] = nightly

    val dockerImageTag = nightly
        .map { if (it) "latest" else projectVersion }
        .map { "konfigyr/$projectName:$it" }
    extra["dockerImageTag"] = dockerImageTag

    apply(plugin = "idea")
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "checkstyle")
    apply(plugin = "project-report")
    apply(plugin = "io.freefair.lombok")

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
        mavenLocal()
    }

    group = "com.konfigyr"
    version = "1.0.0"

    configure<CheckstyleExtension> {
        toolVersion = "13.4.0"
    }

    configure<LombokExtension> {
        version.set("1.18.44")
    }

    dependencies {
        "implementation"(platform(catalog.spring.boot.bom))
        "implementation"(platform(catalog.spring.cloud.bom))
        "implementation"(platform(catalog.spring.modulith.bom))
        "implementation"(platform(catalog.jmolecules.bom))
        "implementation"(platform(catalog.konfigyr.crypto.bom))

        // Apply BOMs to annotation processor classpath so processors can be resolved without versions
        "annotationProcessor"(platform(catalog.spring.boot.bom))

        constraints {
            "implementation"(catalog.bouncy.castle.provider)
            "implementation"(catalog.bouncy.castle.pkix)
            "implementation"(catalog.jsoup)
            "implementation"(catalog.greenmail)
            "implementation"(catalog.wiremock)
            "implementation"(catalog.commons.compress)
            "implementation"(catalog.guava)
            "implementation"(catalog.commonmark.core)
            "implementation"(catalog.commonmark.autolink)
            "implementation"(catalog.commonmark.strikethrough)
            "implementation"(catalog.commonmark.tasklist)
            "implementation"(catalog.owasp.sanitizer)
            "implementation"(catalog.jgit)
        }

        "annotationProcessor"("org.springframework.boot:spring-boot-autoconfigure-processor")
        "annotationProcessor"("org.springframework.boot:spring-boot-configuration-processor")

        "implementation"("com.github.spotbugs:spotbugs-annotations:4.10.2")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    configure<JavaPluginExtension> {
        withJavadocJar()
        withSourcesJar()
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    tasks.withType<Test>().configureEach { useJUnitPlatform() }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:-missing", "-quiet")
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        reports {
            xml.required = true
            html.required = false
        }
    }

    tasks.named("check") { dependsOn("jacocoTestReport") }
}
