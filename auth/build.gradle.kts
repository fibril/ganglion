plugins {
    kotlin("jvm") version "1.9.0"
    id("ganglion.default")
}

dependencies {
    implementation(libs.vertx.core)
    implementation(libs.vertx.auth.jwt)
    implementation(libs.guice)
    implementation(libs.bcrypt)
    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

