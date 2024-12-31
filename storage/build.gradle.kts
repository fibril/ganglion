plugins {
    kotlin("jvm") version "1.9.0"
    alias(libs.plugins.flyway) apply false
    alias(libs.plugins.shadow)
    id("ganglion.default")
}


dependencies {
    api(libs.pg.client)
    api(libs.mysql.client)
    implementation(libs.vertx.config)
    implementation(libs.vertx.kotlin.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.flyway.core)
    implementation(libs.flyway.pg)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
