plugins {
    kotlin("jvm") version "1.9.0"
    alias(libs.plugins.shadow)
    id("ganglion.default")
}


dependencies {
    api(libs.vertx.pg.client)
    api(libs.vertx.redis.client)
    implementation(libs.vertx.config)
    implementation(libs.vertx.kotlin.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.guice)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
