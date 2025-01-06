plugins {
    kotlin("jvm") version "1.9.0"
    alias(libs.plugins.shadow)
    id("ganglion.default")
}


dependencies {
    api(libs.pg.client)
    implementation(libs.vertx.config)
    implementation(libs.vertx.kotlin.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.guice)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
