pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "ganglion"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("ganglion-storage")
include("app")
include("storage")
