import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.bmuschko.docker)
}

val extension = the<KonfigyrBuildExtension>()

docker {
    registryCredentials {
        url.set("https://index.docker.io/v1/")
        username.set("konfigyr")
        password.set(providers.environmentVariable("DOCKER_HUB_TOKEN"))
    }
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

tasks.register<NpmExec>("npmBuild") {
    dependsOn("npmInstall")
    description = "Builds the frontend application"
    group = "build"

    args.set(listOf("run", "build"))
    sources.from(
        "package.json", "package-lock.json", "vite.config.ts", "tsconfig.json",
        fileTree("src"), fileTree("messages"), fileTree("public")
    )
    outputDir.set(layout.projectDirectory.dir(".output"))
}

tasks.register<NpmExec>("npmTest") {
    dependsOn("npmInstall")
    description = "Runs the frontend application tests"
    group = "verification"

    args.set(extension.ci.map { if (it) listOf("run", "test:ci") else listOf("run", "test:coverage") })
    sources.from(
        "package.json", "package-lock.json", "eslint.config.mjs", "vitest.config.mts",
        fileTree("src"), fileTree("messages"), fileTree("public"), fileTree("test")
    )
    outputFile.set(layout.buildDirectory.file("npm-test.stamp"))
}

tasks.register<DockerBuildImage>("dockerBuild") {
    dependsOn(tasks.named("bootJar"))

    group = "docker"
    description = "Builds the Docker image with a frontend application"

    inputDir.set(project.layout.projectDirectory)
    images.add(extension.dockerImageTag)
}

tasks.register<DockerPushImage>("dockerPublish") {
    dependsOn(tasks.named("dockerBuild"))

    group = "docker"
    description = "Builds and publishes the Docker image with a frontend application"

    images.add(extension.dockerImageTag)
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
