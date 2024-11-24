plugins {
    kotlin("jvm") version "1.9.0"
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.vertx.config)
}