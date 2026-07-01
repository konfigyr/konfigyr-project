import io.freefair.gradle.plugins.lombok.LombokExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    alias(libs.plugins.lombok)      apply false
    alias(libs.plugins.spring.boot) apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

// Capture the catalog at root level so it is accessible as a closed-over variable
// inside subprojects {} without being resolved against each subproject's extensions.
val catalog = libs

// Disables all the Java jar related tasks for the root project
tasks.withType<Jar>().configureEach {
    enabled = false
}

subprojects {
    extensions.create("konfigyr", KonfigyrBuildExtension::class, project.name, project.version.toString())

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
        implementation(platform(catalog.spring.boot.bom))
        implementation(platform(catalog.spring.cloud.bom))
        implementation(platform(catalog.spring.modulith.bom))
        implementation(platform(catalog.jmolecules.bom))
        implementation(platform(catalog.konfigyr.crypto.bom))

        // Apply BOMs to annotation processor classpath so processors can be resolved without versions
        annotationProcessor(platform(catalog.spring.boot.bom))
        annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
        annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")

        // Version constraints for libraries that appear only as transitive dependencies and
        // are never directly referenced in subproject build files. Direct dependencies must
        // use libs.<alias> instead so their version is visible at the call site.
        constraints {
            // Pulled in transitively by testcontainers; pinned to suppress CVE alerts.
            implementation(catalog.commons.compress)
            // Pulled in transitively by the checkstyle tool JVM; pinned to suppress CVE alerts.
            implementation(catalog.guava)
        }
    }

    configure<JavaPluginExtension> {
        withJavadocJar()
        withSourcesJar()
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

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
