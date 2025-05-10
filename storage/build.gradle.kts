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
    implementation(libs.ongres.scram.client)
    implementation(libs.guice)
    implementation(libs.opensearch.java)
    implementation(libs.httpclient5)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
