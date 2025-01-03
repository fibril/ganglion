plugins {
    kotlin("jvm") version "1.9.0"
    id("ganglion.default")
}

dependencies {
    implementation(libs.vertx.core)
    implementation(libs.vertx.web)
    implementation(libs.vertx.json.schema)
    implementation(libs.resilience4j.ratelimiter)
    implementation(libs.resilience4j.kotlin)
    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

