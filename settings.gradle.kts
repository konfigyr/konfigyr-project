pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("gradle/versions.toml"))
        }
    }
}

rootProject.name = "konfigyr-project"

include(
    "konfigyr-api",
    "konfigyr-core",
    "konfigyr-data",
    "konfigyr-frontend",
    "konfigyr-identity",
    "konfigyr-jooq-extensions",
    "konfigyr-mail",
    "konfigyr-test"
)
