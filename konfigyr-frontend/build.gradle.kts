import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.github.gradle.node.npm.task.NpmTask

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.node.gradle)
    alias(libs.plugins.bmuschko.docker)
}

val ci = extra["ci"] as Provider<Boolean>
val nightly = extra["nightly"] as Provider<Boolean>
val dockerImageTag = extra["dockerImageTag"] as Provider<String>

docker {
    registryCredentials {
        url.set("https://index.docker.io/v1/")
        username.set("konfigyr")
        password.set(providers.environmentVariable("DOCKER_HUB_TOKEN").orElse(""))
    }
}

node {
    npmCommand.set(providers.gradleProperty("node.npm").get())
    npmInstallCommand.set(ci.map { if (it) "ci" else "install" })
}

tasks.register<NpmTask>("npmBuild") {
    dependsOn(tasks.named("npmInstall"))

    description = "Builds the frontend application"

    args.set(listOf("run", "build"))

    inputs.files("package.json", "package-lock.json", "vite.config.ts", "tsconfig.json")
    inputs.dir("src")
    inputs.dir("messages")
    inputs.dir("public")

    outputs.cacheIf { true }

    outputs.dir(project.layout.buildDirectory.dir(".output"))
}

tasks.register<NpmTask>("npmTest") {
    dependsOn(tasks.named("npmInstall"))

    description = "Runs the frontend application tests"

    args.set(ci.map { if (it) listOf("run", "test:ci") else listOf("run", "test:coverage") })

    inputs.files("package.json", "package-lock.json", "eslint.config.mjs", "vitest.config.mts")
    inputs.dir("src")
    inputs.dir("messages")
    inputs.dir("public")
    inputs.dir("test")
}

tasks.register<DockerBuildImage>("dockerBuild") {
    dependsOn(tasks.named("bootJar"))

    group = "docker"
    description = "Builds the Docker image with a frontend application"

    inputDir.set(project.layout.projectDirectory)
    images.add(dockerImageTag)
}

tasks.register<DockerPushImage>("dockerPublish") {
    dependsOn(tasks.named("dockerBuild"))

    group = "docker"
    description = "Builds and publishes the Docker image with a frontend application"

    images.add(dockerImageTag)
}

tasks.named("bootJar") {
    enabled = false
}

tasks.named("bootBuildImage") {
    enabled = false
    finalizedBy(tasks.named("dockerPublish"))
}

tasks.named("assemble") { dependsOn("npmBuild") }
tasks.named("test") { dependsOn("npmTest") }
