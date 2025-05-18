plugins {
    kotlin("jvm") version "1.9.0"
    id("ganglion.default")
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.vertx.core)
    implementation(libs.vertx.web)
    implementation(libs.vertx.kotlin.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.vertx.json.schema)
    implementation(libs.resilience4j.ratelimiter)
    implementation(libs.resilience4j.kotlin)
    implementation(libs.guice)
    implementation(libs.bcrypt)
    implementation(libs.opensearch.java)
    implementation(project(":auth"))
    implementation(project(":storage"))
    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

