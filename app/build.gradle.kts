plugins {
    kotlin("jvm") version "1.9.0"
    application
    alias(libs.plugins.shadow)
    id("ganglion.default")
    id("io.fibril.ganglion.storage.migration.generate-migration-plugin")
}

repositories {
    mavenCentral()
}

val mainVerticleName = "io.fibril.ganglion.app.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

application {
    mainClass.set(launcherClassName)
}

dependencies {
    implementation(libs.vertx.core)
    implementation(libs.vertx.kotlin.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.vertx.config)
    implementation(project(":storage"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    args = listOf(
        "run",
        mainVerticleName,
        "--launcher-class=$launcherClassName"
    )
}

tasks.named("generateMigration").configure {
    dependsOn("processResources")
}